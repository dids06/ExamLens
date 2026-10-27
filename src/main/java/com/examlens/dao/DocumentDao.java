package com.examlens.dao;

import com.examlens.model.Document;
import com.examlens.model.DocumentBlock;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DocumentDao {

    public long insert(long userId, Long examId, String originalFilename, String storedFilename, String contentType,
                       long fileSize, String sha256, String documentType) throws SQLException {
        return Sql.insert("""
                INSERT INTO documents (user_id, exam_id, original_filename, stored_filename, content_type, file_size,
                  sha256, document_type, extraction_status, extracted_chars, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, userId);
            Sql.setLong(ps, 2, examId);
            ps.setString(3, originalFilename);
            ps.setString(4, storedFilename);
            ps.setString(5, contentType);
            ps.setLong(6, fileSize);
            ps.setString(7, sha256);
            ps.setString(8, documentType);
        });
    }

    public void updateExtraction(long documentId, String status, String method, Integer pageCount,
                                 String warnings, int extractedChars) throws SQLException {
        Sql.update("""
                UPDATE documents SET extraction_status = ?, extraction_method = ?, page_count = ?,
                  extraction_warnings = ?, extracted_chars = ?
                WHERE id = ?
                """, ps -> {
            ps.setString(1, status);
            Sql.setString(ps, 2, method);
            Sql.setInt(ps, 3, pageCount);
            Sql.setString(ps, 4, warnings);
            ps.setInt(5, extractedChars);
            ps.setLong(6, documentId);
        });
    }

    public Optional<Document> find(long id, long userId) throws SQLException {
        return Sql.one("SELECT * FROM documents WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, this::map);
    }

    public List<Document> listForExam(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM documents WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::map);
    }

    public List<Document> listForUser(long userId, int limit) throws SQLException {
        return Sql.list("SELECT * FROM documents WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", ps -> {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
        }, this::map);
    }

    public List<Document> listByType(long examId, long userId, String documentType) throws SQLException {
        return Sql.list("""
                SELECT * FROM documents WHERE exam_id = ? AND user_id = ? AND document_type = ? ORDER BY created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setString(3, documentType);
        }, this::map);
    }

    public void delete(long id, long userId) throws SQLException {
        Sql.update("DELETE FROM documents WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
    }

    public int countForUser(long userId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM documents WHERE user_id = ?", ps -> ps.setLong(1, userId),
                rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    // ------------------------------------------------------------ blocks

    public void replaceBlocks(long documentId, List<DocumentBlock> blocks) throws SQLException {
        Sql.transaction(c -> {
            try (java.sql.PreparedStatement del = c.prepareStatement("DELETE FROM document_blocks WHERE document_id = ?")) {
                del.setLong(1, documentId);
                del.executeUpdate();
            }
            try (java.sql.PreparedStatement ins = c.prepareStatement("""
                    INSERT INTO document_blocks (document_id, block_key, page_number, heading, block_text,
                      extraction_method, confidence, uncertain)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (DocumentBlock b : blocks) {
                    ins.setLong(1, documentId);
                    ins.setString(2, b.getBlockKey());
                    Sql.setInt(ins, 3, b.getPageNumber());
                    Sql.setString(ins, 4, b.getHeading());
                    ins.setString(5, b.getBlockText());
                    ins.setString(6, b.getExtractionMethod());
                    if (b.getConfidence() == null) {
                        ins.setNull(7, java.sql.Types.DECIMAL);
                    } else {
                        ins.setDouble(7, b.getConfidence());
                    }
                    ins.setBoolean(8, b.isUncertain());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    public List<DocumentBlock> blocks(long documentId, long userId) throws SQLException {
        return Sql.list("""
                SELECT b.* FROM document_blocks b
                JOIN documents d ON d.id = b.document_id
                WHERE b.document_id = ? AND d.user_id = ?
                ORDER BY b.page_number, b.id
                """, ps -> {
            ps.setLong(1, documentId);
            ps.setLong(2, userId);
        }, rs -> {
            DocumentBlock b = new DocumentBlock();
            b.setId(rs.getLong("id"));
            b.setDocumentId(rs.getLong("document_id"));
            b.setBlockKey(rs.getString("block_key"));
            b.setPageNumber(Sql.getInt(rs, "page_number"));
            b.setHeading(rs.getString("heading"));
            b.setBlockText(rs.getString("block_text"));
            b.setExtractionMethod(rs.getString("extraction_method"));
            b.setConfidence(Sql.getDouble(rs, "confidence"));
            b.setUncertain(rs.getBoolean("uncertain"));
            b.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return b;
        });
    }

    public int blockCount(long documentId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM document_blocks WHERE document_id = ?",
                ps -> ps.setLong(1, documentId), rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private Document map(java.sql.ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getLong("id"));
        d.setUserId(rs.getLong("user_id"));
        d.setExamId(Sql.getLong(rs, "exam_id"));
        d.setOriginalFilename(rs.getString("original_filename"));
        d.setStoredFilename(rs.getString("stored_filename"));
        d.setContentType(rs.getString("content_type"));
        d.setFileSize(rs.getLong("file_size"));
        d.setSha256(rs.getString("sha256"));
        d.setDocumentType(rs.getString("document_type"));
        d.setPageCount(Sql.getInt(rs, "page_count"));
        d.setExtractionStatus(rs.getString("extraction_status"));
        d.setExtractionMethod(rs.getString("extraction_method"));
        d.setExtractionWarnings(rs.getString("extraction_warnings"));
        d.setExtractedChars(rs.getInt("extracted_chars"));
        d.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        return d;
    }
}
