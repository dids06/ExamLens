package com.examlens.web;

import com.examlens.ai.AiException;
import com.examlens.model.Exam;
import com.examlens.security.Csrf;
import com.examlens.security.SessionUser;
import com.examlens.service.ServiceException;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Shared behaviour for ExamLens controllers: authenticated principal lookup, flash messages,
 * CSRF/view attributes, and uniform translation of service and AI failures into honest messages.
 */
public abstract class ServletSupport extends HttpServlet {

    protected AppContext ctx() {
        return AppContext.get();
    }

    protected SessionUser user(HttpServletRequest req) {
        Object value = req.getSession().getAttribute(SessionUser.SESSION_KEY);
        return value instanceof SessionUser su ? su : null;
    }

    protected long userId(HttpServletRequest req) {
        SessionUser su = user(req);
        if (su == null) {
            throw new ServiceException("Your session has expired. Please sign in again.");
        }
        return su.getId();
    }

    protected String view(String name) {
        return "/WEB-INF/jsp/" + name + ".jsp";
    }

    protected void render(HttpServletRequest req, HttpServletResponse res, String viewName)
            throws ServletException, IOException {
        consumeFlash(req);
        req.setAttribute("csrfToken", Csrf.token(req));
        req.setAttribute("currentUser", user(req));
        req.setAttribute("aiAvailable", ctx().ai().isAiAvailable());
        req.setAttribute("aiSimulated", ctx().ai().isSimulated());
        req.setAttribute("aiProviderMessage", ctx().ai().unavailabilityMessage());
        req.getRequestDispatcher(view(viewName)).forward(req, res);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
        res.sendRedirect(req.getContextPath() + Web.safeRedirect(path, "/dashboard"));
    }

    /**
     * Resolves the exam a page is working on from {@code ?examId=} or the user's active exam.
     * On failure it flashes a message and returns null; the caller should stop.
     */
    protected Exam resolveExam(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long examId = Web.nullableId(req, "examId");
        try {
            if (examId != null && examId > 0) {
                return ctx().exams().require(userId(req), examId);
            }
            Exam active = ctx().exams().active(userId(req)).orElse(null);
            if (active == null) {
                Web.error(req, "Create an exam first to use this page.");
                redirect(req, res, "/exams");
            }
            return active;
        } catch (ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, "/exams");
            return null;
        }
    }

    protected void consumeFlash(HttpServletRequest req) {
        var session = req.getSession();
        move(session, req, Web.FLASH_SUCCESS);
        move(session, req, Web.FLASH_ERROR);
        move(session, req, Web.FLASH_INFO);
    }

    private void move(jakarta.servlet.http.HttpSession session, HttpServletRequest req, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            req.setAttribute(key, value);
            session.removeAttribute(key);
        }
    }

    /**
     * Runs a state-changing action with POST-redirect-GET. On success the supplier returns the
     * redirect target; on a domain or AI failure it flashes an accurate error and redirects to
     * {@code errorRedirect}.
     */
    protected void action(HttpServletRequest req, HttpServletResponse res, String errorRedirect,
                          Supplier<String> work) throws IOException {
        try {
            String target = work.get();
            redirect(req, res, target == null ? errorRedirect : target);
        } catch (ServiceException e) {
            Web.error(req, e.getUserMessage());
            redirect(req, res, errorRedirect);
        } catch (AiException e) {
            Web.error(req, aiMessage(e));
            redirect(req, res, errorRedirect);
        } catch (RuntimeException e) {
            log("Action failed", e);
            Web.error(req, "Something went wrong and the action was not completed. Please try again.");
            redirect(req, res, errorRedirect);
        }
    }

    protected String aiMessage(AiException e) {
        String message = e.getUserMessage();
        String diagnostic = e.getDiagnostic();
        if (diagnostic != null && !diagnostic.isBlank() && e.getFailureType() == com.examlens.ai.AiFailureType.SCHEMA_INVALID) {
            message = message + " Details: " + diagnostic;
        }
        return message;
    }
}
