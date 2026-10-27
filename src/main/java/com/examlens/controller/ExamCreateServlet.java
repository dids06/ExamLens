package com.examlens.controller;

import com.examlens.exception.DataAccessException;
import com.examlens.exception.ValidationException;
import com.examlens.filter.AuthenticationFilter;
import com.examlens.model.Exam;
import com.examlens.model.User;
import com.examlens.service.ExamService;
import com.examlens.service.impl.ExamServiceImpl;
import com.examlens.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Controller handling exam workspace creation at GET/POST /exams/create.
 */
@WebServlet(name = "ExamCreateServlet", urlPatterns = "/exams/create")
public class ExamCreateServlet extends HttpServlet {

    private static final String CREATE_VIEW = "/WEB-INF/views/exam-create.jsp";
    private final ExamService examService;

    public ExamCreateServlet() {
        this(new ExamServiceImpl());
    }

    public ExamCreateServlet(ExamService examService) {
        this.examService = examService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher(CREATE_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Validate CSRF token
        if (!CsrfUtil.isValid(request)) {
            request.setAttribute("errorMessage", "Security check failed: Invalid or expired token. Please try again.");
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher(CREATE_VIEW).forward(request, response);
            return;
        }

        User currentUser = (User) session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE);

        String subjectName = request.getParameter("subjectName");
        String examDateTimeRaw = request.getParameter("examDateTime");
        String dailyMinutesRaw = request.getParameter("dailyStudyMinutes");
        String targetMarksRaw = request.getParameter("targetMarks");

        Exam exam = new Exam();
        exam.setSubjectName(subjectName);

        try {
            if (examDateTimeRaw != null && !examDateTimeRaw.isBlank()) {
                try {
                    exam.setExamDateTime(LocalDateTime.parse(examDateTimeRaw.trim()));
                } catch (DateTimeParseException e) {
                    throw new ValidationException("Invalid exam date/time format. Please select a valid date and time.");
                }
            }

            if (dailyMinutesRaw != null && !dailyMinutesRaw.isBlank()) {
                try {
                    exam.setDailyStudyMinutes(Integer.parseInt(dailyMinutesRaw.trim()));
                } catch (NumberFormatException e) {
                    throw new ValidationException("Daily study minutes must be a valid integer number.");
                }
            }

            if (targetMarksRaw != null && !targetMarksRaw.isBlank()) {
                try {
                    exam.setTargetMarks(new BigDecimal(targetMarksRaw.trim()));
                } catch (NumberFormatException e) {
                    throw new ValidationException("Target marks must be a valid decimal number.");
                }
            }

            Exam createdExam = examService.createExam(currentUser.getId(), exam);

            // Redirect to newly created exam view
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + createdExam.getId());

        } catch (ValidationException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("subjectName", subjectName);
            request.setAttribute("examDateTime", examDateTimeRaw);
            request.setAttribute("dailyStudyMinutes", dailyMinutesRaw);
            request.setAttribute("targetMarks", targetMarksRaw);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher(CREATE_VIEW).forward(request, response);

        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", "Unable to create exam due to a database issue. Please try again later.");
            request.setAttribute("subjectName", subjectName);
            request.setAttribute("examDateTime", examDateTimeRaw);
            request.setAttribute("dailyStudyMinutes", dailyMinutesRaw);
            request.setAttribute("targetMarks", targetMarksRaw);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher(CREATE_VIEW).forward(request, response);
        }
    }
}
