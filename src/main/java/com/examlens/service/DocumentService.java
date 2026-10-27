package com.examlens.service;

import com.examlens.dao.DocumentDao;
import com.examlens.model.Document;
import com.examlens.model.DocumentBlock;
import com.examlens.util.Json;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Upload, extraction and retrieval of study documents. Extracted text is stored as blocks with
 * page provenance; unreadable uploads are recorded honestly as NO_TEXT rather than faked.
 */
public class DocumentService {

    private static final java.util.Set<String> TYPES = java.util.Set.of(
            "SYLLABUS", "TEACHER_NOTES", "STUDY_MATERIAL", "PREVIOUS_PAPER", "QUESTION_BANK", "OTHER");

    private final DocumentDao documents;
    private final StorageService storage;
    private final DocumentExtractionService extraction;
    private final ExamService exams;

    public DocumentService(DocumentDao documents, StorageService storage,
                           DocumentExtractionService extraction, ExamService exams) {
        this.documents = documents;
        this.storage = storage;
        this.extraction = extraction;
        this.exams = exams;
    }

    public record UploadResult(long documentId, String status, String method, int chars,
                               List<String> warnings, boolean readable) {
    }

    public UploadResult upload(long userId, long examId, String documentType, String originalFilename,
                               String contentType, InputStream in) {
        exams.require(userId, examId);
        String safeType = normaliseType(documentType);
        StorageService.StoredFile stored = storage.save(in, originalFilename, contentType);
        long documentId;
        try {
            documentId = documents.insert(userId, examId, stored.originalFilename(), stored.storedFilename(),
                    stored.contentType(), stored.size(), stored.sha256(), safeType);
        } catch (SQLException e) {
            storage.delete(stored.storedFilename());
            throw new ServiceException("The document could not be registered.", e);
        }
        try {
            DocumentExtractionService.Extraction result = extraction.extract(stored.path(), stored.originalFilename());
            documents.replaceBlocks(documentId, result.blocks());
            String status = result.readable() ? "EXTRACTED" : "NO_TEXT";
            documents.updateExtraction(documentId, status, result.method(), result.pageCount(),
                    Json.write(result.warnings()), result.chars());
            return new UploadResult(documentId, status, result.method(), result.chars(),
                    result.warnings(), result.readable());
        } catch (SQLException e) {
            throw new ServiceException("The document was stored but its text could not be saved.", e);
        } catch (RuntimeException e) {
            try {
                documents.updateExtraction(documentId, "FAILED", null, null,
                        "Extraction error: " + e.getMessage(), 0);
            } catch (SQLException ignored) {
                // best-effort status update
            }
            throw new ServiceException("That document could not be read: " + e.getMessage(), e);
        }
    }

    public List<Document> list(long userId, long examId) {
        exams.require(userId, examId);
        try {
            return documents.listForExam(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load your documents.", e);
        }
    }

    public Optional<Document> find(long userId, long documentId) {
        if (documentId <= 0) {
            return Optional.empty();
        }
        try {
            return documents.find(documentId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load that document.", e);
        }
    }

    public List<DocumentBlock> blocks(long userId, long documentId) {
        try {
            return documents.blocks(documentId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load extracted text.", e);
        }
    }

    public void delete(long userId, long documentId) {
        Document document = find(userId, documentId)
                .orElseThrow(() -> new ServiceException("That document was not found."));
        try {
            documents.delete(documentId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not delete the document.", e);
        }
        storage.delete(document.getStoredFilename());
    }

    private static String normaliseType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return "OTHER";
        }
        String upper = documentType.trim().toUpperCase(java.util.Locale.ROOT);
        return TYPES.contains(upper) ? upper : "OTHER";
    }
}
