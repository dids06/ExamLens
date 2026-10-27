package com.examlens.controller;

import com.examlens.exception.DataAccessException;
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
import java.util.List;

/**
 * Controller handling authenticated landing view at GET /dashboard.
 * Displays student profile information and lists student's exam workspaces.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = "/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final String DASHBOARD_VIEW = "/WEB-INF/views/dashboard.jsp";
    private final ExamService examService;

    public DashboardServlet() {
        this(new ExamServiceImpl());
    }

    public DashboardServlet(ExamService examService) {
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

        User user = (User) session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE);
        request.setAttribute("currentUser", user);

        try {
            List<Exam> exams = examService.getExams(user.getId());
            request.setAttribute("exams", exams);
        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", "Unable to load exams from database. Please verify MySQL connectivity.");
        }

        // Surface query parameter alerts if present
        if (request.getParameter("errorMessage") != null) {
            request.setAttribute("errorMessage", request.getParameter("errorMessage"));
        }
        if (request.getParameter("successMessage") != null) {
            request.setAttribute("successMessage", request.getParameter("successMessage"));
        }

        // Supply CSRF token for the logout button and other forms
        request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));

        request.getRequestDispatcher(DASHBOARD_VIEW).forward(request, response);
    }
}
