package com.examlens.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** Rejects state-changing requests that do not carry a valid CSRF token. */
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (SAFE_METHODS.contains(req.getMethod().toUpperCase()) || Csrf.isValid(req)) {
            chain.doFilter(request, response);
            return;
        }
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"CSRF_INVALID\",\"message\":\"Your session token expired. Reload the page and try again.\"}");
        } else {
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write("<!doctype html><html><head><title>Request rejected</title>"
                    + "<link rel=\"stylesheet\" href=\"" + req.getContextPath() + "/assets/css/app.css\"></head>"
                    + "<body class=\"auth-body\"><main class=\"auth-card\"><h1>Request rejected</h1>"
                    + "<p>Your security token was missing or expired. Please reload the page and try again.</p>"
                    + "<p><a class=\"btn btn-primary\" href=\"" + req.getContextPath() + "/dashboard\">Back to dashboard</a></p>"
                    + "</main></body></html>");
        }
    }
}
