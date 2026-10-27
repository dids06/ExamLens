package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.model.ReadinessAssessment;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * P15 — readiness analysis. Java computes the canonical metrics (completion, minutes, practice
 * average) from persisted evidence and the AI interprets them and selects weak topics.
 */
@WebServlet(name = "readiness", urlPatterns = {"/readiness", "/readiness/*"})
public class ReadinessServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/readiness")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render(req, res, "error/404");
            return;
        }
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        long userId = userId(req);
        ReadinessAssessment assessment = ctx().readiness().latest(userId, exam.getId()).orElse(null);
        req.setAttribute("exam", exam);
        req.setAttribute("assessment", assessment);
        int[] stats = ctx().plan().taskStats(userId, exam.getId());
        req.setAttribute("taskTotal", stats[0]);
        req.setAttribute("taskDone", stats[1]);
        req.setAttribute("plannedMinutes", ctx().plan().plannedMinutes(userId, exam.getId()));
        req.setAttribute("actualMinutes", ctx().sessions().actualMinutes(userId, exam.getId()));
        req.setAttribute("practiceAverage", ctx().practice().practiceAverage(userId, exam.getId()));
        req.setAttribute("paperCount", ctx().practice().countPapers(userId, exam.getId()));
        render(req, res, "readiness/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/readiness?examId=" + examId;
        switch (path) {
            case "/readiness/analyse" -> action(req, res, back, () -> {
                ctx().readiness().analyse(userId(req), examId);
                Web.success(req, "Readiness assessment updated from your recorded activity.");
                return back;
            });
            default -> redirect(req, res, back);
        }
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
