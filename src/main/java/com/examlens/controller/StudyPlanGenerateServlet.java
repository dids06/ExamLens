package com.examlens.controller;

import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.filter.AuthenticationFilter;
import com.examlens.model.StudyPlan;
import com.examlens.model.User;
import com.examlens.service.StudyPlanService;
import com.examlens.service.impl.StudyPlanServiceImpl;
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

/**
 * Controller handling study plan generation at POST /study-plan/generate.
 * Contract: Section 8 /study-plan/generate (POST).
 */
@WebServlet(name = "StudyPlanGenerateServlet", urlPatterns = "/study-plan/generate")
public class StudyPlanGenerateServlet extends HttpServlet {

    private final StudyPlanService studyPlanService;

    public StudyPlanGenerateServlet() {
        this(new StudyPlanServiceImpl());
    }

    public StudyPlanGenerateServlet(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 1. Parse examId
        String examIdRaw = request.getParameter("examId");
        if (examIdRaw == null || examIdRaw.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        Long examId;
        try {
            examId = Long.parseLong(examIdRaw.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // 2. CSRF Verification
        if (!CsrfUtil.isValid(request)) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                    "&errorMessage=" + URLEncoder.encode("Security check failed: Invalid or expired token. Please try again.", StandardCharsets.UTF_8));
            return;
        }

        User currentUser = (User) session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE);

        try {
            // 3. Generate initial plan
            StudyPlan plan = studyPlanService.generateInitialPlan(examId, currentUser.getId());

            // 4. Redirect to exam view
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId + "&planGenerated=true");

        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId + "&errorMessage=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));

        } catch (AuthorizationException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode("Access denied: You do not have permission to modify that exam workspace.", StandardCharsets.UTF_8));

        } catch (ResourceNotFoundException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode("The requested exam workspace does not exist.", StandardCharsets.UTF_8));

        } catch (DataAccessException e) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId + "&errorMessage=" +
                    URLEncoder.encode("A database error occurred while creating the study plan. Please try again.", StandardCharsets.UTF_8));
        }
    }
}
