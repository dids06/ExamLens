package com.examlens.web;

import com.examlens.model.Document;
import com.examlens.model.Exam;
import com.examlens.service.DocumentService;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.List;

/** Upload, list and inspect study documents. Files are stored outside the web root. */
@WebServlet(name = "documents", urlPatterns = {"/documents", "/documents/*"})
@MultipartConfig(maxFileSize = 25L * 1024 * 1024, maxRequestSize = 30L * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
public class DocumentServlet extends ServletSupport {

    private static final List<String> TYPES = List.of(
            "SYLLABUS", "TEACHER_NOTES", "STUDY_MATERIAL", "PREVIOUS_PAPER", "QUESTION_BANK", "OTHER");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        switch (path) {
            case "/documents" -> {
                Exam exam = resolveExam(req, res);
                if (exam == null) {
                    return;
                }
                req.setAttribute("exam", exam);
                req.setAttribute("documents", ctx().documents().list(userId(req), exam.getId()));
                req.setAttribute("types", TYPES);
                req.setAttribute("uploadMaxMb", ctx().config().uploadMaxMb());
                render(req, res, "documents/list");
            }
            case "/documents/view" -> view(req, res);
            default -> {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                render(req, res, "error/404");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String path = path(req);
        switch (path) {
            case "/documents/upload" -> upload(req, res);
            case "/documents/delete" -> delete(req, res);
            default -> redirect(req, res, "/documents");
        }
    }

    private void upload(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        long examId = Web.longParam(req, "examId", 0);
        String documentType = Web.param(req, "documentType", "OTHER");
        String back = "/documents?examId=" + examId;
        Part part;
        try {
            part = req.getPart("file");
        } catch (IllegalStateException e) {
            Web.error(req, "That file is larger than the " + ctx().config().uploadMaxMb() + " MB upload limit.");
            redirect(req, res, back);
            return;
        }
        if (part == null || part.getSize() == 0) {
            Web.error(req, "Choose a file to upload.");
            redirect(req, res, back);
            return;
        }
        action(req, res, back, () -> {
            String submitted = part.getSubmittedFileName();
            if (submitted == null || submitted.isBlank()) {
                throw new com.examlens.service.ServiceException("That upload has no filename.");
            }
            try (var in = part.getInputStream()) {
                DocumentService.UploadResult result = ctx().documents()
                        .upload(userId(req), examId, documentType, submitted, part.getContentType(), in);
                if (result.readable()) {
                    Web.success(req, "Uploaded \"" + submitted + "\" and extracted " + result.chars()
                            + " characters (" + result.method() + ").");
                } else {
                    Web.error(req, "Uploaded \"" + submitted + "\", but no readable text was found. "
                            + String.join(" ", result.warnings()));
                }
            } catch (IOException e) {
                throw new com.examlens.service.ServiceException(
                        "The upload could not be read. Please try again.");
            }
            return back;
        });
    }

    private void delete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        long documentId = Web.longParam(req, "documentId", 0);
        action(req, res, "/documents", () -> {
            ctx().documents().delete(userId(req), documentId);
            Web.success(req, "Document deleted.");
            return "/documents";
        });
    }

    private void view(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Long id = Web.nullableId(req, "id");
        if (id == null || id <= 0) {
            Web.error(req, "That document was not found.");
            redirect(req, res, "/documents");
            return;
        }
        Document document = ctx().documents().find(userId(req), id).orElse(null);
        if (document == null) {
            Web.error(req, "That document was not found.");
            redirect(req, res, "/documents");
            return;
        }
        req.setAttribute("document", document);
        req.setAttribute("blocks", ctx().documents().blocks(userId(req), id));
        render(req, res, "documents/view");
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
