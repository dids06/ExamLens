package com.examlens.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter that protects authenticated routes specified in the shared contract.
 * Unauthenticated requests are redirected to /login.
 */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {
        "/dashboard",
        "/exams/*",
        "/topics/*",
        "/study-plan/*",
        "/tasks/*",
        "/documents/*",
        "/papers/*",
        "/attempts/*"
})
public class AuthenticationFilter implements Filter {

    public static final String SESSION_USER_ATTRIBUTE = "user";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute(SESSION_USER_ATTRIBUTE) != null);

        if (loggedIn) {
            // Attach user to request attribute for convenient access in JSPs
            httpRequest.setAttribute("currentUser", session.getAttribute(SESSION_USER_ATTRIBUTE));
            chain.doFilter(request, response);
        } else {
            // Prevent caching of protected pages
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);

            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
        }
    }
}
