package com.examlens.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/** Requires an authenticated session for every path except explicitly public ones. */
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/auth/", "/assets/", "/health", "/favicon.ico", "/WEB-INF/jsp/error/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        boolean authenticated = session != null && session.getAttribute(SessionUser.SESSION_KEY) != null;
        if (!authenticated) {
            if (wantsJson(req)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"AUTH_REQUIRED\",\"message\":\"Please sign in to continue.\"}");
            } else {
                req.getSession(true).setAttribute("postLoginRedirect", safePath(req));
                res.sendRedirect(req.getContextPath() + "/auth/login?reason=required");
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (path.equals("/")) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json"))
                || "fetch".equalsIgnoreCase(requestedWith);
    }

    static String safePath(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        String query = req.getQueryString();
        if (uri.startsWith("//") || uri.contains("://")) {
            return "/dashboard";
        }
        return query == null || query.isBlank() ? uri : uri + "?" + query;
    }
}
