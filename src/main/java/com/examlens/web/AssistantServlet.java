package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** ASSISTANT_INTAKE and P13 — the doubt chat and concept explanations. */
@WebServlet(name = "assistant", urlPatterns = {"/assistant", "/assistant/*"})
public class AssistantServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/assistant")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render(req, res, "error/404");
            return;
        }
        long userId = userId(req);
        Long examId = Web.nullableId(req, "examId");
        req.setAttribute("exams", ctx().exams().list(userId));
        req.setAttribute("selectedExamId", examId);
        req.setAttribute("conversations", ctx().assistant().conversations(userId));
        Long conversationId = Web.nullableId(req, "conversationId");
        if (conversationId != null && conversationId > 0) {
            req.setAttribute("conversationId", conversationId);
            req.setAttribute("messages", ctx().assistant().messages(userId, conversationId, 200));
        }
        req.setAttribute("explanations", ctx().assistant().explanations(userId, examId, 10));
        render(req, res, "assistant/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        Long examId = Web.nullableId(req, "examId");
        String back = "/assistant" + (examId == null ? "" : "?examId=" + examId);
        switch (path) {
            case "/assistant/chat" -> action(req, res, back, () -> {
                var reply = ctx().assistant().chat(userId(req), examId, req.getParameter("message"),
                        Web.param(req, "depth"));
                return "/assistant?conversationId=" + reply.conversationId()
                        + (examId == null ? "" : "&examId=" + examId);
            });
            case "/assistant/explain" -> action(req, res, back, () -> {
                ctx().assistant().explain(userId(req), examId, req.getParameter("question"),
                        Web.param(req, "topic"), Web.param(req, "depth"));
                Web.success(req, "Explanation generated. It has been added to your explanation history.");
                return back;
            });
            case "/assistant/delete" -> action(req, res, back, () -> {
                ctx().assistant().deleteConversation(userId(req), Web.longParam(req, "conversationId", 0));
                Web.success(req, "Conversation deleted.");
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
