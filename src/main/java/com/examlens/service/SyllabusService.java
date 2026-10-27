package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.DocDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.DocumentDao;
import com.examlens.dao.SyllabusDao;
import com.examlens.model.Document;
import com.examlens.model.DocumentBlock;
import com.examlens.model.Exam;
import com.examlens.model.SyllabusClassification;
import com.examlens.model.SyllabusVersion;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * P01 — syllabus extraction and the human review/confirm workflow. Topics are drafts until the
 * student confirms them; AI page references are only accepted when the source is genuinely paginated.
 */
public class SyllabusService {

    private final DocumentDao documents;
    private final SyllabusDao syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public SyllabusService(DocumentDao documents, SyllabusDao syllabus, ExamService exams, AiServiceRunner ai) {
        this.documents = documents;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
    }

    public record Draft(SyllabusVersion version, List<Topic> topics, List<SyllabusClassification> classifications) {
        public SyllabusVersion getVersion() { return version; }
        public List<Topic> getTopics() { return topics; }
        public List<SyllabusClassification> getClassifications() { return classifications; }
    }

    // ------------------------------------------------------------------ extraction

    public long extract(long userId, long examId, long documentId) {
        Exam exam = exams.require(userId, examId);
        Document document = findDocument(userId, documentId);
        if (document.getExamId() == null || document.getExamId() != examId) {
            throw new ServiceException("That document is not attached to this exam.");
        }
        List<DocumentBlock> blocks = blocks(userId, documentId);
        if (blocks.isEmpty()) {
            throw new ServiceException("This document has no extracted text yet. Run extraction first.");
        }
        boolean pageLabelled = blocks.stream().anyMatch(b -> b.getPageNumber() != null);
        Integer maxPage = document.getPageCount();

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.put("sourceDocumentId", String.valueOf(documentId));
        input.put("hasPageMetadata", pageLabelled);
        if (maxPage != null) {
            input.put("pageCount", maxPage);
        }
        input.set("blocks", AiInputs.blocks(blocks));
        input.put("rawText", AiInputs.rawText(blocks));

        AiOutcome<DocDtos.SyllabusExtractionResult> outcome = ai.run(
                AiAction.P01_SYLLABUS_EXTRACTION, userId, examId, input,
                AiValidators.syllabusExtraction(),
                data -> domainCheck(data, pageLabelled, maxPage));

        return persistDraft(userId, examId, documentId, outcome);
    }

    private List<String> domainCheck(DocDtos.SyllabusExtractionResult data, boolean pageLabelled, Integer maxPage) {
        List<String> errors = new ArrayList<>();
        int topicCount = 0;
        for (DocDtos.SyllabusExtractionResult.Unit unit : data.units) {
            for (DocDtos.SyllabusExtractionResult.TopicDraft topic : unit.topics) {
                topicCount++;
                if (!pageLabelled && topic.sourcePages != null && !topic.sourcePages.isEmpty()) {
                    errors.add("topic \"" + topic.title + "\" reports source pages, but the document has no page metadata. "
                            + "Remove sourcePages for unpaginated content.");
                }
                if (pageLabelled && maxPage != null && topic.sourcePages != null) {
                    for (Integer page : topic.sourcePages) {
                        if (page == null || page < 1 || page > maxPage) {
                            errors.add("topic \"" + topic.title + "\" references page " + page
                                    + " outside the document range 1.." + maxPage + ".");
                        }
                    }
                }
            }
        }
        if (topicCount == 0) {
            errors.add("no topics were produced; return at least one unit with at least one topic, or report that no syllabus content was found.");
        }
        if (!pageLabelled) {
            for (DocDtos.SyllabusExtractionResult.Classification c : data.classifications) {
                if (c.page != null) {
                    errors.add("classification for \"" + shorten(c.originalText) + "\" reports a page, but the document has no page metadata.");
                }
            }
        } else if (maxPage != null) {
            for (DocDtos.SyllabusExtractionResult.Classification c : data.classifications) {
                if (c.page != null && (c.page < 1 || c.page > maxPage)) {
                    errors.add("classification for \"" + shorten(c.originalText) + "\" references page " + c.page
                            + " outside the document range 1.." + maxPage + ".");
                }
            }
        }
        return errors;
    }

    private long persistDraft(long userId, long examId, long documentId,
                              AiOutcome<DocDtos.SyllabusExtractionResult> outcome) {
        DocDtos.SyllabusExtractionResult result = outcome.getData();
        try {
            int versionNumber = syllabus.nextVersionNumber(examId);
            long versionId = syllabus.insertVersion(examId, userId, versionNumber, "DRAFT", documentId,
                    outcome.getJobId(), result.summary, Json.write(result.warnings));
            int order = 0;
            for (DocDtos.SyllabusExtractionResult.Unit unit : result.units) {
                for (DocDtos.SyllabusExtractionResult.TopicDraft draft : unit.topics) {
                    Topic topic = new Topic();
                    topic.setSyllabusVersionId(versionId);
                    topic.setExamId(examId);
                    topic.setUnitName(unit.unitName);
                    topic.setTitle(draft.title);
                    topic.setSubtopics(draft.subtopics == null ? null : String.join("\n", draft.subtopics));
                    topic.setLearningOutcomes(draft.learningOutcomes == null ? null : String.join("\n", draft.learningOutcomes));
                    topic.setImportance(draft.importance);
                    topic.setDifficulty(draft.difficulty);
                    topic.setEstimatedMinutes(draft.estimatedMinutes);
                    topic.setSourcePages(draft.sourcePages == null || draft.sourcePages.isEmpty()
                            ? null : joinPages(draft.sourcePages));
                    topic.setConfidence(draft.confidence);
                    topic.setNeedsReview(draft.needsReview == null || draft.needsReview || draft.reason != null);
                    topic.setAiReason(draft.reason);
                    topic.setDisplayOrder(++order);
                    syllabus.insertTopic(topic);
                }
            }
            for (DocDtos.SyllabusExtractionResult.Classification c : result.classifications) {
                SyllabusClassification classification = new SyllabusClassification();
                classification.setSyllabusVersionId(versionId);
                classification.setBlockKey(c.sourceBlockId);
                classification.setPageNumber(c.page);
                classification.setOriginalText(shorten(c.originalText, 2000));
                classification.setClassification(c.classification);
                classification.setConfidence(c.confidence);
                classification.setNormalizedTopic(c.normalizedTopic);
                classification.setParentUnit(c.parentUnit);
                classification.setReason(c.reason);
                classification.setIncluded(c.includeInSyllabus != null && c.includeInSyllabus);
                syllabus.insertClassification(classification);
            }
            return versionId;
        } catch (SQLException e) {
            throw new ServiceException("The extracted syllabus could not be saved.", e);
        }
    }

    // ------------------------------------------------------------------ review

    public Draft loadDraft(long userId, long examId) {
        SyllabusVersion version = latestVersion(examId, userId, null)
                .orElseThrow(() -> new ServiceException("No syllabus has been extracted for this exam yet."));
        return loadDraft(userId, version);
    }

    public Draft loadDraftVersion(long userId, long versionId) {
        SyllabusVersion version = findVersion(versionId, userId)
                .orElseThrow(() -> new ServiceException("That syllabus version was not found."));
        return loadDraft(userId, version);
    }

    private Draft loadDraft(long userId, SyllabusVersion version) {
        try {
            List<Topic> topics = syllabus.topicsForVersion(version.getId(), userId);
            List<SyllabusClassification> classifications = syllabus.classifications(version.getId(), userId);
            return new Draft(version, topics, classifications);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the syllabus review.", e);
        }
    }

    public List<SyllabusVersion> versions(long userId, long examId) {
        try {
            return syllabus.versions(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load syllabus versions.", e);
        }
    }

    public void confirm(long userId, long examId, long versionId) {
        SyllabusVersion version = findVersion(versionId, userId)
                .orElseThrow(() -> new ServiceException("That syllabus version was not found."));
        if (version.getExamId() != examId) {
            throw new ServiceException("That syllabus version belongs to a different exam.");
        }
        try {
            int count = syllabus.confirmedTopics(examId, userId).size();
            if (version.getStatus() != null && !"CONFIRMED".equals(version.getStatus()) && count == 0) {
                // verifying the version actually has topics before confirming it
                if (syllabus.topicsForVersion(versionId, userId).isEmpty()) {
                    throw new ServiceException("Add at least one topic before confirming the syllabus.");
                }
            }
            syllabus.confirmVersion(versionId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not confirm the syllabus.", e);
        }
    }

    public void updateTopic(long userId, long examId, long topicId, String title, String unitName,
                            String subtopics, String learningOutcomes, String importance, String difficulty,
                            Integer estimatedMinutes, Integer displayOrder, boolean needsReview) {
        exams.require(userId, examId);
        if (title == null || title.isBlank()) {
            throw new ServiceException("A topic title is required.");
        }
        String safeImportance = normaliseEnum(importance, List.of("HIGH", "MEDIUM", "LOW"));
        String safeDifficulty = normaliseEnum(difficulty, List.of("HARD", "MEDIUM", "EASY"));
        if (estimatedMinutes != null && (estimatedMinutes < 5 || estimatedMinutes > 1440)) {
            throw new ServiceException("Estimated minutes must be between 5 and 1440.");
        }
        try {
            Optional<Topic> existing = syllabus.findTopic(topicId, userId);
            if (existing.isEmpty() || existing.get().getExamId() != examId) {
                throw new ServiceException("That topic was not found for this exam.");
            }
            int order = displayOrder == null ? existing.get().getDisplayOrder() : displayOrder;
            syllabus.updateTopic(topicId, examId, userId, title.trim(), unitName == null ? null : unitName.trim(),
                    subtopics, learningOutcomes, safeImportance, safeDifficulty, estimatedMinutes, order, needsReview);
        } catch (SQLException e) {
            throw new ServiceException("Could not update the topic.", e);
        }
    }

    public void addTopic(long userId, long examId, String title, String unitName, String importance, String difficulty,
                         Integer estimatedMinutes) {
        exams.require(userId, examId);
        if (title == null || title.isBlank()) {
            throw new ServiceException("A topic title is required.");
        }
        try {
            Optional<SyllabusVersion> draft = latestVersion(examId, userId, "DRAFT");
            SyllabusVersion version = draft.isPresent() ? draft.get()
                    : latestVersion(examId, userId, null)
                        .orElseThrow(() -> new ServiceException("Extract a syllabus before adding topics."));
            Topic topic = new Topic();
            topic.setSyllabusVersionId(version.getId());
            topic.setExamId(examId);
            topic.setUnitName(unitName == null || unitName.isBlank() ? null : unitName.trim());
            topic.setTitle(title.trim());
            topic.setImportance(normaliseEnum(importance, List.of("HIGH", "MEDIUM", "LOW")));
            topic.setDifficulty(normaliseEnum(difficulty, List.of("HARD", "MEDIUM", "EASY")));
            topic.setEstimatedMinutes(estimatedMinutes);
            topic.setConfidence("HIGH");
            topic.setNeedsReview(false);
            topic.setAiReason("Added manually by the student.");
            topic.setDisplayOrder(syllabus.maxDisplayOrder(version.getId()) + 1);
            syllabus.insertTopic(topic);
        } catch (SQLException e) {
            throw new ServiceException("Could not add the topic.", e);
        }
    }

    public void deleteTopic(long userId, long examId, long topicId) {
        exams.require(userId, examId);
        try {
            syllabus.deleteTopic(topicId, examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not delete the topic.", e);
        }
    }

    public List<Topic> confirmedTopics(long userId, long examId) {
        try {
            return syllabus.confirmedTopics(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load confirmed topics.", e);
        }
    }

    public int countConfirmedTopics(long userId, long examId) {
        try {
            return syllabus.countConfirmedTopics(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not count confirmed topics.", e);
        }
    }

    public List<DocumentBlock> blocks(long userId, long documentId) {
        try {
            return documents.blocks(documentId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load extracted text.", e);
        }
    }

    private Document findDocument(long userId, long documentId) {
        try {
            return documents.find(documentId, userId)
                    .orElseThrow(() -> new ServiceException("That document was not found in your account."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load that document.", e);
        }
    }

    private Optional<SyllabusVersion> latestVersion(long examId, long userId, String status) {
        try {
            return syllabus.latestVersion(examId, userId, status);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the syllabus version.", e);
        }
    }

    private Optional<SyllabusVersion> findVersion(long versionId, long userId) {
        try {
            return syllabus.findVersion(versionId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the syllabus version.", e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String normaliseEnum(String value, List<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase(java.util.Locale.ROOT);
        return allowed.contains(upper) ? upper : null;
    }

    private static String joinPages(List<Integer> pages) {
        List<String> parts = new ArrayList<>();
        for (Integer p : pages) {
            if (p != null) {
                parts.add(String.valueOf(p));
            }
        }
        return parts.isEmpty() ? null : String.join(",", parts);
    }

    private static String shorten(String s) {
        return shorten(s, 60);
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    static ArrayNode emptyArray() {
        return Json.array();
    }
}
