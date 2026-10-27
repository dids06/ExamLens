package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.model.PaperAttempt;
import com.examlens.model.PaperQuestion;
import com.examlens.model.PracticePaper;
import com.examlens.service.PracticeService;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P09 (mock paper), P10 (practice set), P11 (answer keys) and P12 (subjective evaluation).
 * Multiple-choice grading is deterministic and server-side; subjective grading is the only part
 * the AI performs for a submission.
 */
@WebServlet(name = "practice", urlPatterns = {"/practice", "/practice/*"})
public class PracticeServlet extends ServletSupport {

    private static final List<String> TYPES = List.of("MCQ", "SHORT_ANSWER", "THEORY", "NUMERICAL", "APPLICATION");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        switch (path(req)) {
            case "/practice" -> index(req, res);
            case "/practice/new" -> newPaper(req, res);
            case "/practice/paper" -> paper(req, res);
            case "/practice/attempt" -> attempt(req, res);
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
        String back = "/practice?examId=" + examId;
        switch (path) {
            case "/practice/generate" -> action(req, res, back, () -> {
                PracticeService.PaperConfig config = new PracticeService.PaperConfig(
                        nullableInt(req, "totalMarks"), nullableInt(req, "questionCount"),
                        nullableInt(req, "durationMinutes"), Web.param(req, "difficulty"),
                        typeParams(req), longParams(req, "topicIds"));
                long paperId = ctx().practice().generateMockPaper(userId(req), examId, config);
                Web.success(req, "The AI generated your mock paper. Prepare the answer key to see model answers.");
                return "/practice/paper?id=" + paperId;
            });
            case "/practice/set" -> action(req, res, back, () -> {
                int count = Web.intParam(req, "count", 10);
                long paperId = ctx().practice().generatePracticeSet(userId(req), examId, count,
                        Web.param(req, "difficulty"), typeParams(req), longParams(req, "topicIds"));
                Web.success(req, "The AI generated a focused practice set on the topics you selected.");
                return "/practice/paper?id=" + paperId;
            });
            case "/practice/solutions" -> action(req, res, back, () -> {
                long paperId = Web.longParam(req, "paperId", 0);
                ctx().practice().prepareSolutions(userId(req), paperId);
                Web.success(req, "Model answers and rubrics are ready for this paper.");
                return "/practice/paper?id=" + paperId;
            });
            case "/practice/start" -> action(req, res, back, () -> {
                long paperId = Web.longParam(req, "paperId", 0);
                boolean timed = Web.boolParam(req, "timed");
                long attemptId = ctx().practice().startAttempt(userId(req), paperId, timed,
                        nullableInt(req, "durationMinutes"));
                return "/practice/attempt?id=" + attemptId;
            });
            case "/practice/submit" -> submit(req, res);
            default -> redirect(req, res, back);
        }
    }

    private void index(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        long userId = userId(req);
        req.setAttribute("exam", exam);
        req.setAttribute("papers", ctx().practice().papers(userId, exam.getId()));
        req.setAttribute("attempts", ctx().practice().attempts(userId, exam.getId(), 25));
        req.setAttribute("paperCount", ctx().practice().countPapers(userId, exam.getId()));
        req.setAttribute("practiceAverage", ctx().practice().practiceAverage(userId, exam.getId()));
        req.setAttribute("types", TYPES);
        render(req, res, "practice/index");
    }

    private void newPaper(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        req.setAttribute("exam", exam);
        req.setAttribute("topics", ctx().syllabus().confirmedTopics(userId(req), exam.getId()));
        req.setAttribute("types", TYPES);
        render(req, res, "practice/new");
    }

    private void paper(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Long paperId = Web.nullableId(req, "id");
        if (paperId == null || paperId <= 0) {
            Web.error(req, "That practice paper was not found.");
            redirect(req, res, "/practice");
            return;
        }
        long userId = userId(req);
        PracticePaper paper;
        try {
            paper = ctx().practice().requirePaper(userId, paperId);
        } catch (com.examlens.service.ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/practice");
            return;
        }
        req.setAttribute("exam", ctx().exams().require(userId, paper.getExamId()));
        req.setAttribute("paper", paper);
        req.setAttribute("attempts", ctx().practice().attemptsForPaper(userId, paper.getId()));
        render(req, res, "practice/paper");
    }

    private void attempt(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Long attemptId = Web.nullableId(req, "id");
        if (attemptId == null || attemptId <= 0) {
            Web.error(req, "That attempt was not found.");
            redirect(req, res, "/practice");
            return;
        }
        long userId = userId(req);
        PracticeService.AttemptView view;
        try {
            view = ctx().practice().viewAttempt(userId, attemptId).orElse(null);
        } catch (com.examlens.service.ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/practice");
            return;
        }
        if (view == null) {
            Web.error(req, "That attempt was not found.");
            redirect(req, res, "/practice");
            return;
        }
        req.setAttribute("view", view);
        req.setAttribute("paper", view.paper());
        req.setAttribute("exam", ctx().exams().require(userId, view.paper().getExamId()));
        render(req, res, "practice/attempt");
    }

    private void submit(HttpServletRequest req, HttpServletResponse res) throws IOException {
        long attemptId = Web.longParam(req, "attemptId", 0);
        String back = "/practice/attempt?id=" + attemptId;
        action(req, res, back, () -> {
            PaperAttempt attempt = ctx().practice().requireAttempt(userId(req), attemptId);
            PracticePaper paper = attempt.getPaper() != null
                    ? attempt.getPaper()
                    : ctx().practice().requirePaper(userId(req), attempt.getPaperId());
            Map<Long, String> answers = new LinkedHashMap<>();
            for (PaperQuestion q : paper.getQuestions()) {
                String value = req.getParameter("q_" + q.getId());
                if (value != null && !value.isBlank()) {
                    answers.put(q.getId(), value.trim());
                }
            }
            ctx().practice().submitAttempt(userId(req), attemptId, answers);
            Web.success(req, "Attempt submitted. Multiple-choice questions were graded on the server; "
                    + "written answers were graded by the AI against the rubric.");
            return back;
        });
    }

    private List<String> typeParams(HttpServletRequest req) {
        String[] values = req.getParameterValues("types");
        List<String> out = new ArrayList<>();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    private List<Long> longParams(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        List<Long> out = new ArrayList<>();
        if (values != null) {
            for (String v : values) {
                try {
                    out.add(Long.parseLong(v.trim()));
                } catch (NumberFormatException ignored) {
                    // ignore malformed topic ids; the service rejects unknown topics on its own
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
