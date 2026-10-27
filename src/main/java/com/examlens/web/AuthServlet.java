package com.examlens.web;

import com.examlens.model.User;
import com.examlens.security.SessionUser;
import com.examlens.service.ServiceException;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Sign-in, registration and POST-only logout. The session id is renewed on authentication. */
@WebServlet(name = "auth", urlPatterns = {"/auth/*"})
public class AuthServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        switch (path) {
            case "/auth/login" -> {
                if (user(req) != null) {
                    redirect(req, res, "/dashboard");
                    return;
                }
                req.setAttribute("reason", Web.param(req, "reason"));
                render(req, res, "auth/login");
            }
            case "/auth/register" -> {
                if (user(req) != null) {
                    redirect(req, res, "/dashboard");
                    return;
                }
                render(req, res, "auth/register");
            }
            case "/auth/logout" -> render(req, res, "auth/logout");
            default -> {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                render(req, res, "error/404");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        switch (path) {
            case "/auth/login" -> login(req, res);
            case "/auth/register" -> register(req, res);
            case "/auth/logout" -> logout(req, res);
            default -> redirect(req, res, "/auth/login");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String email = Web.param(req, "email");
        String password = req.getParameter("password");
        try {
            User user = ctx().auth().authenticate(email, password);
            establishSession(req, user);
            String target = Web.safeRedirect((String) req.getSession().getAttribute("postLoginRedirect"), "/dashboard");
            req.getSession().removeAttribute("postLoginRedirect");
            Web.success(req, "Welcome back, " + user.getDisplayName() + ".");
            redirect(req, res, target);
        } catch (ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/auth/login");
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String email = Web.param(req, "email");
        String name = Web.param(req, "displayName");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        String timezone = Web.param(req, "timezone", "UTC");
        try {
            long userId = ctx().auth().register(email, name, password, confirm, timezone);
            User user = ctx().auth().require(userId);
            establishSession(req, user);
            Web.success(req, "Your ExamLens account is ready. Start by creating your exam.");
            redirect(req, res, "/exams");
        } catch (ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/auth/register");
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        HttpSession fresh = req.getSession(true);
        fresh.setAttribute(Web.FLASH_INFO, "You have been signed out.");
        redirect(req, res, "/auth/login");
    }

    /** Defeats session fixation: the pre-login session is discarded and a new one is created. */
    private void establishSession(HttpServletRequest req, User user) {
        HttpSession existing = req.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getTimezone(), user.getTheme()));
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
