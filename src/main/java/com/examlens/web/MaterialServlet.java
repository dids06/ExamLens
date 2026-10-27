package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** P03 — study-material indexing and P04 — source comparison. */
@WebServlet(name = "materials", urlPatterns = {"/materials", "/materials/*"})
public class MaterialServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/materials")) {
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
        req.setAttribute("indexes", ctx().materials().indexes(userId(req), exam.getId()));
        req.setAttribute("comparison", ctx().materials().latestComparison(userId(req), exam.getId()).orElse(null));
        render(req, res, "materials/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/materials?examId=" + examId;
        switch (path) {
            case "/materials/index" -> action(req, res, back, () -> {
                ctx().materials().index(userId(req), examId, Web.longParam(req, "documentId", 0));
                Web.success(req, "Material indexed. Coverage is based only on the supplied source text.");
                return back;
            });
            case "/materials/compare" -> action(req, res, back, () -> {
                ctx().materials().compare(userId(req), examId);
                Web.success(req, "Sources compared. Recommendations cite the supplied sources only.");
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
