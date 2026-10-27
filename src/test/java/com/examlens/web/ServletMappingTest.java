package com.examlens.web;

import jakarta.servlet.annotation.WebServlet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards a subtle but severe packaging rule: a servlet mapped to "/" becomes the context's default
 * servlet and shadows Tomcat's own default servlet, so static resources under {@code /assets/**}
 * stop being served (they hit the app servlet instead). The landing route must therefore use the
 * empty-string pattern, which matches the context root exactly.
 */
class ServletMappingTest {

    /** Every servlet in the web layer. Kept explicit so a new servlet is a conscious addition. */
    private static final List<Class<?>> SERVLETS = List.of(
            AssistantServlet.class,
            AuthServlet.class,
            DashboardServlet.class,
            DocumentServlet.class,
            ExamServlet.class,
            GuidanceServlet.class,
            HealthServlet.class,
            HomeServlet.class,
            MaterialServlet.class,
            PlanServlet.class,
            PracticeServlet.class,
            PyqServlet.class,
            ReadinessServlet.class,
            RevisionServlet.class,
            SessionServlet.class,
            SettingsServlet.class,
            SyllabusServlet.class);

    @Test
    void noServletClaimsTheDefaultPattern() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> servlet : SERVLETS) {
            WebServlet mapping = servlet.getAnnotation(WebServlet.class);
            if (mapping == null) {
                continue;
            }
            for (String pattern : mapping.urlPatterns()) {
                if ("/".equals(pattern)) {
                    offenders.add(servlet.getSimpleName());
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "These servlets map to \"/\" and would shadow Tomcat's default servlet, breaking "
                        + "static assets: " + offenders);
    }

    @Test
    void landingRouteUsesTheExactContextRootPattern() {
        WebServlet mapping = HomeServlet.class.getAnnotation(WebServlet.class);
        assertTrue(mapping != null, "HomeServlet must declare @WebServlet");
        boolean hasContextRoot = false;
        for (String pattern : mapping.urlPatterns()) {
            assertFalse("/".equals(pattern), "HomeServlet must not claim the default servlet pattern");
            if (pattern.isEmpty()) {
                hasContextRoot = true;
            }
        }
        assertTrue(hasContextRoot, "HomeServlet must map to \"\" so it matches the context root only");
    }
}
