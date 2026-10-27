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
import java.util.List;

/**
 * Account, study-preference and AI-status settings. Secrets are never rendered: the AI panel shows
 * only whether a provider is configured, never the key itself.
 */
@WebServlet(name = "settings", urlPatterns = {"/settings", "/settings/*"})
public class SettingsServlet extends ServletSupport {

    private static final List<String> TIMEZONES = List.of(
            "UTC", "Europe/London", "Europe/Paris", "Europe/Berlin", "America/New_York",
            "America/Chicago", "America/Los_Angeles", "Asia/Kolkata", "Asia/Dubai",
            "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!path(req).equals("/settings")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render(req, res, "error/404");
            return;
        }
        long userId = userId(req);
        User user = ctx().auth().require(userId);
        req.setAttribute("account", user);
        req.setAttribute("study", ctx().auth().settings(userId));
        req.setAttribute("timezones", TIMEZONES);
        req.setAttribute("configSummary", ctx().config().safeSummary());
        req.setAttribute("aiProvider", ctx().config().aiProvider());
        req.setAttribute("aiModel", ctx().config().geminiModel());
        req.setAttribute("geminiConfigured", !ctx().config().geminiApiKey().isBlank());
        render(req, res, "settings/index");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        switch (path) {
            case "/settings/profile" -> action(req, res, "/settings", () -> {
                ctx().auth().updateProfile(userId(req), Web.param(req, "displayName"),
                        Web.param(req, "timezone", "UTC"), Web.param(req, "theme", "system"));
                refreshSession(req);
                Web.success(req, "Profile updated.");
                return "/settings";
            });
            case "/settings/password" -> action(req, res, "/settings", () -> {
                ctx().auth().changePassword(userId(req), req.getParameter("currentPassword"),
                        req.getParameter("newPassword"), req.getParameter("confirmPassword"));
                Web.success(req, "Password changed.");
                return "/settings";
            });
            case "/settings/study" -> action(req, res, "/settings", () -> {
                ctx().auth().updateSettings(userId(req), Web.intParam(req, "defaultStudyMinutes", 120),
                        Web.param(req, "preferredDepth", "STANDARD"), Web.intParam(req, "retentionDays", 365));
                Web.success(req, "Study preferences saved.");
                return "/settings";
            });
            case "/settings/theme" -> {
                try {
                    ctx().auth().updateTheme(userId(req), Web.param(req, "theme", "system"));
                    refreshSession(req);
                    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } catch (ServiceException e) {
                    log("Theme sync failed", e);
                    res.setStatus(HttpServletResponse.SC_OK);
                }
            }
            case "/settings/delete" -> deleteAccount(req, res);
            default -> redirect(req, res, "/settings");
        }
    }

    private void deleteAccount(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String confirm = Web.param(req, "confirmDelete");
        if (confirm == null || !confirm.equalsIgnoreCase("DELETE")) {
            Web.error(req, "Type DELETE to confirm deleting your account.");
            redirect(req, res, "/settings");
            return;
        }
        try {
            ctx().auth().deleteAccount(userId(req));
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            HttpSession fresh = req.getSession(true);
            fresh.setAttribute(Web.FLASH_INFO, "Your account and all of its records were deleted.");
            redirect(req, res, "/auth/login");
        } catch (ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/settings");
        }
    }

    /** Re-reads the account so the header and theme reflect profile changes immediately. */
    private void refreshSession(HttpServletRequest req) {
        User user = ctx().auth().require(userId(req));
        HttpSession session = req.getSession();
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getTimezone(), user.getTheme()));
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
