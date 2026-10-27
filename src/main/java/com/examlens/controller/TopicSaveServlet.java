package com.examlens.controller;

import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.filter.AuthenticationFilter;
import com.examlens.model.Topic;
import com.examlens.model.User;
import com.examlens.service.TopicService;
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

/**
 * Controller handling topic creation at POST /topics/save.
 */
@WebServlet(name = "TopicSaveServlet", urlPatterns = "/topics/save")
public class TopicSaveServlet extends HttpServlet {

    private final TopicService topicService;

    public TopicSaveServlet() {
        this(new TopicServiceImpl());
    }

    public TopicSaveServlet(TopicService topicService) {
        this.topicService = topicService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

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

        // Validate CSRF token
        if (!CsrfUtil.isValid(request)) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                    "&errorMessage=" + URLEncoder.encode("Security check failed: Invalid or expired token. Please try again.", StandardCharsets.UTF_8));
            return;
        }

        User currentUser = (User) session.getAttribute(AuthenticationFilter.SESSION_USER_ATTRIBUTE);

        String name = request.getParameter("name");
        String estMinutesRaw = request.getParameter("estimatedMinutes");
        String priority = request.getParameter("priority");
        String parentTopicIdRaw = request.getParameter("parentTopicId");

        Topic topic = new Topic();
        topic.setName(name);
        topic.setPriority(priority);

        if (estMinutesRaw != null && !estMinutesRaw.isBlank()) {
            try {
                topic.setEstimatedMinutes(Integer.parseInt(estMinutesRaw.trim()));
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                        "&errorMessage=" + URLEncoder.encode("Estimated minutes must be a valid positive integer.", StandardCharsets.UTF_8));
                return;
            }
        }

        if (parentTopicIdRaw != null && !parentTopicIdRaw.isBlank() && !"0".equals(parentTopicIdRaw.trim())) {
            try {
                topic.setParentTopicId(Long.parseLong(parentTopicIdRaw.trim()));
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                        "&errorMessage=" + URLEncoder.encode("Invalid parent topic selection.", StandardCharsets.UTF_8));
                return;
            }
        }

        try {
            topicService.addTopic(examId, currentUser.getId(), topic);
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId + "&topicAdded=true");

        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                    "&errorMessage=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));

        } catch (AuthorizationException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));

        } catch (ResourceNotFoundException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?errorMessage=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));

        } catch (DataAccessException e) {
            response.sendRedirect(request.getContextPath() + "/exams/view?id=" + examId +
                    "&errorMessage=" + URLEncoder.encode("Unable to add topic due to a database error.", StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // State changes require POST per contract
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
