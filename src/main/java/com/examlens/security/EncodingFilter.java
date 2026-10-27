package com.examlens.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/** Forces UTF-8 request encoding before any parameter is read. */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        // Only force the response charset for dynamic (servlet/JSP) responses. For static
        // resources served by Tomcat's default servlet the bytes are already UTF-8 on disk;
        // setting a charset here makes the container transcode them and produces mojibake
        // (e.g. "✓" instead of "?"). JSPs and servlets declare their own charset.
        if (request instanceof HttpServletRequest httpReq) {
            String path = httpReq.getRequestURI();
            String ctx = httpReq.getContextPath();
            String relative = (ctx == null || ctx.isEmpty()) ? path : path.substring(ctx.length());
            if (!relative.startsWith("/assets/")) {
                response.setCharacterEncoding("UTF-8");
            }
        } else {
            response.setCharacterEncoding("UTF-8");
        }
        chain.doFilter(request, response);
    }
}
