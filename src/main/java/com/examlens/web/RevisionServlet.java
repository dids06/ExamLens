package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** P14 — AI-generated revision resources (key points, flashcards and active-recall questions). */
@WebServlet(name = "revision", urlPatterns = {"/revision", "/revision/*"})
public class RevisionServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/revision")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render(req, res, "error/404");
            return;
        }
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        long userId = userId(req);
        req.setAttribute("exam", exam);
        req.setAttribute("topics", ctx().syllabus().confirmedTopics(userId, exam.getId()));
        req.setAttribute("resources", ctx().revision().resources(userId, exam.getId()));
        req.setAttribute("resourceCount", ctx().revision().count(userId, exam.getId()));
        render(req, res, "revision/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/revision?examId=" + examId;
        switch (path) {
            case "/revision/generate" -> action(req, res, back, () -> {
                ctx().revision().generate(userId(req), examId, topicIds(req), Web.param(req, "depth"),
                        nullableInt(req, "availableMinutes"));
                Web.success(req, "Revision resources generated. The AI chose which topics to cover and how deeply.");
                return back;
            });
            default -> redirect(req, res, back);
        }
    }

    private List<Long> topicIds(HttpServletRequest req) {
        String[] values = req.getParameterValues("topicIds");
        List<Long> out = new ArrayList<>();
        if (values != null) {
            for (String v : values) {
                if (v == null) {
                    continue;
                }
                try {
                    out.add(Long.parseLong(v.trim()));
                } catch (NumberFormatException ignored) {
                    // the service ignores topic ids that are not confirmed for the exam
                }
            }
        }
        return out;
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
