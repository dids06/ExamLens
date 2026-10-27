package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** P02 — teacher instructions, hints and important topics (draft until confirmed). */
@WebServlet(name = "guidance", urlPatterns = {"/guidance", "/guidance/*"})
public class GuidanceServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/guidance")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render(req, res, "error/404");
            return;
        }
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        req.setAttribute("exam", exam);
        req.setAttribute("documents", ctx().documents().list(userId(req), exam.getId()));
        req.setAttribute("guidance", ctx().guidance().latest(userId(req), exam.getId()).orElse(null));
        render(req, res, "guidance/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/guidance?examId=" + examId;
        switch (path) {
            case "/guidance/extract" -> action(req, res, back, () -> {
                ctx().guidance().extract(userId(req), examId, Web.longParam(req, "documentId", 0));
                Web.success(req, "Teacher guidance extracted. Review it, then confirm what is correct.");
                return back;
            });
            case "/guidance/confirm" -> action(req, res, back, () -> {
                ctx().guidance().confirm(userId(req), examId, Web.longParam(req, "guidanceId", 0));
                Web.success(req, "Teacher guidance confirmed. It will now inform your plan.");
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
