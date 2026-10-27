package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** The signed-in home screen: real persisted activity for the active exam only. */
@WebServlet(name = "dashboard", urlPatterns = {"/dashboard"})
public class DashboardServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long userId = userId(req);
        List<Exam> exams = ctx().exams().list(userId);
        req.setAttribute("exams", exams);

        Exam exam = ctx().exams().active(userId).orElse(null);
        if (exam != null) {
            req.setAttribute("activeExam", exam);
            int[] stats = ctx().plan().taskStats(userId, exam.getId());
            req.setAttribute("taskTotal", stats[0]);
            req.setAttribute("taskDone", stats[1]);
            req.setAttribute("plannedMinutes", ctx().plan().plannedMinutes(userId, exam.getId()));
            req.setAttribute("actualMinutes", ctx().sessions().actualMinutes(userId, exam.getId()));
            req.setAttribute("practiceAverage", ctx().practice().practiceAverage(userId, exam.getId()));
            req.setAttribute("topicCount", ctx().syllabus().countConfirmedTopics(userId, exam.getId()));
            req.setAttribute("documentCount", ctx().documents().list(userId, exam.getId()).size());
            req.setAttribute("revisionCount", ctx().revision().count(userId, exam.getId()));
            req.setAttribute("paperCount", ctx().practice().countPapers(userId, exam.getId()));
            req.setAttribute("upcomingTasks", ctx().plan().upcoming(userId, exam.getId(), 5));
            req.setAttribute("readiness", ctx().readiness().latest(userId, exam.getId()).orElse(null));
            req.setAttribute("hasPlan", ctx().plan().activeVersion(userId, exam.getId()).isPresent());
            req.setAttribute("latestGuidance", ctx().guidance().latest(userId, exam.getId()).orElse(null));
            try {
                req.setAttribute("pyqAnalysis", ctx().pyq().latestAnalysis(userId, exam.getId()).orElse(null));
            } catch (ServiceException ignored) {
                // the dashboard must still render if a supplementary read fails
            }
        }
        render(req, res, "dashboard");
    }
}
