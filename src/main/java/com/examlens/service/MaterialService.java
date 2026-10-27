package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.DocDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.DocumentDao;
import com.examlens.dao.LearningDao;
import com.examlens.model.Document;
import com.examlens.model.DocumentBlock;
import com.examlens.model.Exam;
import com.examlens.model.MaterialIndex;
import com.examlens.model.SourceComparison;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * P03 — study-material indexing and P04 — source comparison. Page references are only ever
 * produced from genuinely paginated extraction, and comparisons must cite supplied source IDs.
 */
public class MaterialService {

    private final DocumentDao documents;
    private final LearningDao learning;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public MaterialService(DocumentDao documents, LearningDao learning, SyllabusService syllabus,
                           ExamService exams, AiServiceRunner ai) {
        this.documents = documents;
        this.learning = learning;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
    }

    public long index(long userId, long examId, long documentId) {
        exams.require(userId, examId);
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
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);

        ObjectNode input = AiInputs.object();
        input.put("documentName", document.getOriginalFilename());
        input.put("documentId", String.valueOf(documentId));
        input.put("hasPageMetadata", pageLabelled);
        input.set("blocks", AiInputs.blocks(blocks));
        input.put("rawText", AiInputs.rawText(blocks));
        input.set("syllabusTopics", AiInputs.topics(topics));

        AiOutcome<DocDtos.MaterialIndexResult> outcome = ai.run(
                AiAction.P03_STUDY_MATERIAL_INDEXING, userId, examId, input,
                AiValidators.materialIndex(),
                data -> indexDomainCheck(data, pageLabelled, maxPage));

        DocDtos.MaterialIndexResult result = outcome.getData();
        try {
            MaterialIndex index = new MaterialIndex();
            index.setExamId(examId);
            index.setUserId(userId);
            index.setDocumentId(documentId);
            index.setAiJobId(outcome.getJobId());
            index.setSectionsJson(Json.write(result.sections));
            index.setUnmappedTopics(Json.write(result.unmappedTopics));
            index.setWarnings(Json.write(result.warnings));
            index.setSimulated(outcome.isSimulated());
            return learning.upsertMaterialIndex(index);
        } catch (SQLException e) {
            throw new ServiceException("The material index could not be saved.", e);
        }
    }

    private List<String> indexDomainCheck(DocDtos.MaterialIndexResult data, boolean pageLabelled, Integer maxPage) {
        List<String> errors = new ArrayList<>();
        for (DocDtos.MaterialIndexResult.Section section : data.sections) {
            if (!pageLabelled && (section.startPage != null || section.endPage != null)) {
                errors.add("section \"" + section.title + "\" reports page numbers, but the material has no page metadata.");
            }
            if (pageLabelled && maxPage != null && section.endPage != null && section.endPage > maxPage) {
                errors.add("section \"" + section.title + "\" endPage " + section.endPage
                        + " is outside the document range 1.." + maxPage + ".");
            }
        }
        return errors;
    }

    public long compare(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        List<MaterialIndex> indexes = indexes(userId, examId);
        if (indexes.size() < 2) {
            throw new ServiceException("Index at least two study materials before comparing sources.");
        }
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);

        Map<String, String> sourceNames = new HashMap<>();
        ArrayNode sources = Json.array();
        for (MaterialIndex index : indexes) {
            String sourceId = String.valueOf(index.getDocumentId());
            sourceNames.put(sourceId, index.getDocumentName());
            ObjectNode node = AiInputs.object();
            node.put("sourceId", sourceId);
            node.put("name", index.getDocumentName());
            ArrayNode sections = Json.array();
            try {
                if (index.getSectionsJson() != null && !index.getSectionsJson().isBlank()) {
                    sections = (ArrayNode) Json.read(index.getSectionsJson());
                }
            } catch (RuntimeException ignored) {
                // keep empty sections rather than failing the whole comparison
            }
            node.set("sections", sections);
            sources.add(node);
        }

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.set("syllabusTopics", AiInputs.topics(topics));
        input.set("sources", sources);

        AiOutcome<DocDtos.SourceComparisonResult> outcome = ai.run(
                AiAction.P04_SOURCE_COMPARISON, userId, examId, input,
                AiValidators.sourceComparison(),
                data -> compareDomainCheck(data, sourceNames.keySet()));

        DocDtos.SourceComparisonResult result = outcome.getData();
        try {
            SourceComparison comparison = new SourceComparison();
            comparison.setExamId(examId);
            comparison.setUserId(userId);
            comparison.setAiJobId(outcome.getJobId());
            comparison.setRecommendationsJson(Json.write(result.recommendations));
            comparison.setUncoveredTopics(Json.write(result.uncoveredTopics));
            comparison.setWarnings(Json.write(result.warnings));
            comparison.setSimulated(outcome.isSimulated());
            return learning.insertSourceComparison(comparison);
        } catch (SQLException e) {
            throw new ServiceException("The source comparison could not be saved.", e);
        }
    }

    private List<String> compareDomainCheck(DocDtos.SourceComparisonResult data, java.util.Set<String> sourceIds) {
        List<String> errors = new ArrayList<>();
        for (DocDtos.SourceComparisonResult.Recommendation r : data.recommendations) {
            if (r.primarySourceId == null || !sourceIds.contains(r.primarySourceId.trim())) {
                errors.add("recommendation for \"" + r.topic + "\" cites primarySourceId \"" + r.primarySourceId
                        + "\" which is not one of the supplied sources.");
            }
        }
        return errors;
    }

    public List<MaterialIndex> indexes(long userId, long examId) {
        try {
            return learning.materialIndexes(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load material indexes.", e);
        }
    }

    public Optional<SourceComparison> latestComparison(long userId, long examId) {
        try {
            return learning.latestSourceComparison(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the source comparison.", e);
        }
    }

    private List<DocumentBlock> blocks(long userId, long documentId) {
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
}
