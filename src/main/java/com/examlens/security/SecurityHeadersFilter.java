package com.examlens.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Adds defensive security headers to every response. */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse res) {
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("X-Frame-Options", "DENY");
            res.setHeader("Referrer-Policy", "same-origin");
            res.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            res.setHeader("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self'; font-src 'self'; connect-src 'self'; form-action 'self'; "
                    + "base-uri 'self'; object-src 'none'; frame-ancestors 'none'");
            if (request instanceof HttpServletRequest req && req.isSecure()) {
                res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }
        }
        chain.doFilter(request, response);
    }
}
