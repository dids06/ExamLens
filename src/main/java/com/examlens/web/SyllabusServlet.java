package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.service.ServiceException;
import com.examlens.service.SyllabusService;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** P01 — syllabus extraction and the human review/confirm workflow. */
@WebServlet(name = "syllabus", urlPatterns = {"/syllabus", "/syllabus/*"})
public class SyllabusServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        switch (path) {
            case "/syllabus" -> review(req, res);
            default -> {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                render(req, res, "error/404");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        switch (path) {
            case "/syllabus/extract" -> action(req, res, "/syllabus?examId=" + examId, () -> {
                long documentId = Web.longParam(req, "documentId", 0);
                ctx().syllabus().extract(userId(req), examId, documentId);
                Web.success(req, "Syllabus extracted. Review the topics and classifications, then confirm.");
                return "/syllabus?examId=" + examId;
            });
            case "/syllabus/topic/update" -> action(req, res, "/syllabus?examId=" + examId, () -> {
                ctx().syllabus().updateTopic(userId(req), examId, Web.longParam(req, "topicId", 0),
                        Web.param(req, "title"), Web.param(req, "unitName"), Web.param(req, "subtopics"),
                        Web.param(req, "learningOutcomes"), Web.param(req, "importance"), Web.param(req, "difficulty"),
                        nullableInt(req, "estimatedMinutes"), nullableInt(req, "displayOrder"),
                        Web.boolParam(req, "needsReview"));
                Web.success(req, "Topic updated.");
                return "/syllabus?examId=" + examId;
            });
            case "/syllabus/topic/add" -> action(req, res, "/syllabus?examId=" + examId, () -> {
                ctx().syllabus().addTopic(userId(req), examId, Web.param(req, "title"), Web.param(req, "unitName"),
                        Web.param(req, "importance"), Web.param(req, "difficulty"), nullableInt(req, "estimatedMinutes"));
                Web.success(req, "Topic added.");
                return "/syllabus?examId=" + examId;
            });
            case "/syllabus/topic/delete" -> action(req, res, "/syllabus?examId=" + examId, () -> {
                ctx().syllabus().deleteTopic(userId(req), examId, Web.longParam(req, "topicId", 0));
                Web.success(req, "Topic removed.");
                return "/syllabus?examId=" + examId;
            });
            case "/syllabus/confirm" -> action(req, res, "/syllabus?examId=" + examId, () -> {
                ctx().syllabus().confirm(userId(req), examId, Web.longParam(req, "versionId", 0));
                Web.success(req, "Syllabus confirmed. These topics now drive your plan and practice.");
                return "/syllabus?examId=" + examId;
            });
            default -> redirect(req, res, "/syllabus");
        }
    }

    private void review(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        req.setAttribute("exam", exam);
        req.setAttribute("documents", ctx().documents().list(userId(req), exam.getId()));
        req.setAttribute("versions", ctx().syllabus().versions(userId(req), exam.getId()));
        req.setAttribute("confirmedCount", ctx().syllabus().countConfirmedTopics(userId(req), exam.getId()));
        try {
            SyllabusService.Draft draft = ctx().syllabus().loadDraft(userId(req), exam.getId());
            req.setAttribute("draft", draft);
            req.setAttribute("hasDraft", true);
        } catch (ServiceException e) {
            req.setAttribute("hasDraft", false);
        }
        render(req, res, "syllabus/review");
    }

    private Integer nullableInt(HttpServletRequest req, String name) {
        String value = Web.param(req, name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
