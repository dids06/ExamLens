package com.examlens.controller;

import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.filter.AuthenticationFilter;
import com.examlens.model.Exam;
import com.examlens.model.Topic;
import com.examlens.model.User;
import com.examlens.service.ExamService;
import com.examlens.service.TopicService;
import com.examlens.service.impl.ExamServiceImpl;
import com.examlens.service.impl.TopicServiceImpl;
import com.examlens.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller handling display of an exam workspace and its syllabus topics at GET /exams/view?id=...
 */
@WebServlet(name = "ExamViewServlet", urlPatterns = "/exams/view")
public class ExamViewServlet extends HttpServlet {

    private static final String VIEW_JSP = "/WEB-INF/views/exam-view.jsp";
    private final ExamService examService;
    private final TopicService topicService;
    private final com.examlens.service.StudyPlanService studyPlanService;

    public ExamViewServlet() {
        this(new ExamServiceImpl(), new TopicServiceImpl(), new com.examlens.service.impl.StudyPlanServiceImpl());
    }

    public ExamViewServlet(ExamService examService, TopicService topicService) {
        this(examService, topicService, new com.examlens.service.impl.StudyPlanServiceImpl());
    }

    public ExamViewServlet(ExamService examService, TopicService topicService, com.examlens.service.StudyPlanService studyPlanService) {
        this.examService = examService;
        this.topicService = topicService;
        this.studyPlanService = studyPlanService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User currentUser = (User) session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE);

        String idRaw = request.getParameter("id");
        if (idRaw == null || idRaw.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        Long examId;
        try {
            examId = Long.parseLong(idRaw.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            Exam exam = examService.getExam(examId, currentUser.getId());
            List<Topic> topics = topicService.getTopicsForExam(examId, currentUser.getId());
            com.examlens.model.StudyPlan currentPlan = studyPlanService.getCurrentPlan(examId, currentUser.getId());

            request.setAttribute("exam", exam);
            request.setAttribute("topics", topics);
            request.setAttribute("currentPlan", currentPlan);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));

            // Surface flash messages from query parameters if present
            if ("true".equalsIgnoreCase(request.getParameter("topicAdded"))) {
                request.setAttribute("successMessage", "Topic successfully added to syllabus.");
            }
            if ("true".equalsIgnoreCase(request.getParameter("planGenerated"))) {
                request.setAttribute("successMessage", "Initial study plan successfully generated!");
            }
            if (request.getParameter("errorMessage") != null) {
                request.setAttribute("errorMessage", request.getParameter("errorMessage"));
            }

            request.getRequestDispatcher(VIEW_JSP).forward(request, response);

        } catch (AuthorizationException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode("Access denied: You do not have permission to view that exam workspace.", StandardCharsets.UTF_8));

        } catch (ResourceNotFoundException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode("The requested exam workspace does not exist.", StandardCharsets.UTF_8));

        } catch (DataAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode("A database error occurred while loading the exam workspace.", StandardCharsets.UTF_8));
        }
    }
}
