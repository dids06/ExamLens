package com.examlens.controller;

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
 * Controller handling student registration at GET /register and POST /register.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public class RegisterServlet extends HttpServlet {

    private static final String REGISTER_VIEW = "/WEB-INF/views/register.jsp";
    private final UserService userService;

    public RegisterServlet() {
        this(new UserServiceImpl());
    }

    public RegisterServlet(UserService userService) {
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

        // Establish session and supply CSRF token
        HttpSession activeSession = request.getSession(true);
        request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(activeSession));

        request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Enforce CSRF validation on registration submission
        if (!CsrfUtil.isValid(request)) {
            request.setAttribute("errorMessage", "Security check failed: Invalid or expired token. Please try again.");
            HttpSession session = request.getSession(true);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);
            return;
        }

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            User registeredUser = userService.register(name, email, password, confirmPassword);

            // Redirect to login page with success indicator
            response.sendRedirect(request.getContextPath() + "/login?registered=true");

        } catch (ValidationException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("name", name);
            request.setAttribute("email", email);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);

        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", "Unable to complete registration due to a database issue. Please try again later.");
            request.setAttribute("name", name);
            request.setAttribute("email", email);
            request.setAttribute(CsrfUtil.CSRF_PARAM_NAME, CsrfUtil.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);
        }
    }
}
