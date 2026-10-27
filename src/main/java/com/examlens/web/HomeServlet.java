package com.examlens.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Landing route: sends authenticated users to the dashboard and everyone else to sign-in.
 *
 * <p>Mapped to the empty string, which matches the context root exactly. Mapping it to "/" would
 * make it the context's default servlet and shadow Tomcat's own default servlet, so static
 * resources under {@code /assets/**} would never be served.</p>
 */
@WebServlet(name = "home", urlPatterns = {""})
public class HomeServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (user(req) == null) {
            redirect(req, res, "/auth/login");
        } else {
            redirect(req, res, "/dashboard");
        }
    }
}
