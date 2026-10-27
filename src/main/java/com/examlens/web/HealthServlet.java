package com.examlens.web;

import com.examlens.config.Db;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/** Unauthenticated liveness probe. Does not expose configuration or secrets. */
@WebServlet(name = "health", urlPatterns = {"/health"})
public class HealthServlet extends ServletSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        boolean databaseUp;
        try (Connection c = Db.connection()) {
            databaseUp = c.isValid(2);
        } catch (Exception e) {
            databaseUp = false;
        }
        res.setContentType("application/json;charset=UTF-8");
        res.setStatus(databaseUp ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        res.getWriter().write("{\"application\":\"ExamLens\",\"status\":\""
                + (databaseUp ? "ok" : "degraded") + "\",\"database\":\""
                + (databaseUp ? "up" : "down") + "\",\"aiProvider\":\""
                + (ctx().ai().isSimulated() ? "simulated" : "configured") + "\"}");
    }
}
