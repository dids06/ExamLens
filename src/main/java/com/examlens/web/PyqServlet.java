package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** P05 — previous-year paper analysis: questions, topic frequency and honest limitations. */
@WebServlet(name = "pyq", urlPatterns = {"/pyq", "/pyq/*"})
public class PyqServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/pyq")) {
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
        req.setAttribute("documents", ctx().documents().list(userId, exam.getId()));
        req.setAttribute("papers", ctx().pyq().papers(userId, exam.getId()));
        req.setAttribute("analysis", ctx().pyq().latestAnalysis(userId, exam.getId()).orElse(null));
        Long paperId = Web.nullableId(req, "paperId");
        if (paperId != null && paperId > 0) {
            req.setAttribute("selectedPaperId", paperId);
            req.setAttribute("questions", ctx().pyq().questions(userId, paperId));
        }
        render(req, res, "pyq/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        long examId = Web.longParam(req, "examId", 0);
        String back = "/pyq?examId=" + examId;
        switch (path) {
            case "/pyq/paper/add" -> action(req, res, back, () -> {
                ctx().pyq().addPaper(userId(req), examId, Web.longParam(req, "documentId", 0),
                        Web.param(req, "label"), Web.param(req, "year"),
                        com.examlens.service.ExamService.parsePositiveInt(Web.param(req, "totalMarks")));
                Web.success(req, "Previous-year paper added. Run the analysis when you have added them all.");
                return back;
            });
            case "/pyq/analyse" -> action(req, res, back, () -> {
                ctx().pyq().analyse(userId(req), examId);
                Web.success(req, "Analysis complete. Frequency counts come only from the extracted questions.");
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
