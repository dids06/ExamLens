package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.service.SessionService;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Study-session logging plus P16 feedback. The student records what actually happened; the AI
 * only turns those recorded facts into constructive feedback.
 */
@WebServlet(name = "sessions", urlPatterns = {"/sessions", "/sessions/*"})
public class SessionServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/sessions")) {
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
        req.setAttribute("sessions", ctx().sessions().sessions(userId, exam.getId(), 60));
        req.setAttribute("tasks", ctx().plan().loadPlan(userId, exam.getId())
                .map(com.examlens.service.StudyPlanService.PlanView::tasks).orElse(java.util.List.of()));
        req.setAttribute("actualMinutes", ctx().sessions().actualMinutes(userId, exam.getId()));
        req.setAttribute("plannedMinutes", ctx().plan().plannedMinutes(userId, exam.getId()));
        render(req, res, "sessions/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/sessions?examId=" + examId;
        switch (path) {
            case "/sessions/log" -> action(req, res, back, () -> {
                SessionService.SessionResult result = ctx().sessions().log(userId(req), examId,
                        nullableLong(req, "taskId"), nullableInt(req, "plannedMinutes"),
                        Web.intParam(req, "actualMinutes", 0), Web.param(req, "difficultyFeedback"),
                        Web.param(req, "notes"), Web.boolParam(req, "markComplete"));
                Web.success(req, result.feedback() != null && result.feedback().summary != null
                        ? "Session logged. AI feedback: " + result.feedback().summary
                        : "Study session logged.");
                return back;
            });
            default -> redirect(req, res, back);
        }
    }

    private Long nullableLong(HttpServletRequest req, String name) {
        String value = Web.param(req, name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
