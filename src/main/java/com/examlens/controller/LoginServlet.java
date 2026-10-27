package com.examlens.controller;

import com.examlens.exception.AuthenticationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ValidationException;
import com.examlens.filter.AuthenticationFilter;
import com.examlens.model.User;
import com.examlens.service.UserService;
import com.examlens.service.impl.UserServiceImpl;
import com.examlens.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller handling student login at GET /login and POST /login.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final String LOGIN_VIEW = "/WEB-INF/views/login.jsp";
    private final UserService userService;

    public LoginServlet() {
        this(new UserServiceImpl());
    }

    public LoginServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        if ("true".equalsIgnoreCase(request.getParameter("registered"))) {
            request.setAttribute("successMessage", "Account created successfully! Please log in with your credentials.");
        } else if ("true".equalsIgnoreCase(request.getParameter("loggedOut"))) {
            request.setAttribute("infoMessage", "You have been logged out successfully.");
        }

        // Establish session and provide CSRF token
        HttpSession activeSession = request.getSession(true);
        request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(activeSession));

        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Validate CSRF token
        if (!CsrfUtil.isValid(request)) {
            request.setAttribute("errorMessage", "Security check failed: Invalid or expired token. Please try again.");
            HttpSession session = request.getSession(true);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
            return;
        }

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User user = userService.authenticate(email, password);

            // Rotate session ID to mitigate session fixation attacks (Jakarta Servlet standard)
            request.changeSessionId();
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE, user);

            // Establish fresh CSRF token for the authenticated session
            CsrfUtil.rotateToken(session);

            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (AuthenticationException | ValidationException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("email", email);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);

        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", "Unable to log in due to a database error. Please try again later.");
            request.setAttribute("email", email);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
        }
    }
}
