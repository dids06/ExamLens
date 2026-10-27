package com.examlens.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CsrfTest {

    /** Minimal in-memory session so the CSRF logic can be exercised without a container. */
    private static final class FakeSession implements HttpSession {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        @Override
        public void invalidate() {
            attributes.clear();
        }

        // Only the members used by Csrf are implemented; the rest are unused in these tests.
        @Override public long getCreationTime() { return 0; }
        @Override public String getId() { return "fake"; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) { }
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override public java.util.Enumeration<String> getAttributeNames() {
            return java.util.Collections.enumeration(attributes.keySet());
        }
        @Override public boolean isNew() { return false; }
    }

    @Test
    void tokenIsStableWithinASession() {
        HttpSession session = new FakeSession();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession()).thenReturn(session);

        String first = Csrf.token(request);
        String second = Csrf.token(request);
        assertNotNull(first);
        assertFalse(first.isBlank());
        assertSame(first, second);
    }

    @Test
    void distinctSessionsGetDistinctTokens() {
        HttpServletRequest a = Mockito.mock(HttpServletRequest.class);
        when(a.getSession(true)).thenReturn(new FakeSession());
        HttpServletRequest b = Mockito.mock(HttpServletRequest.class);
        when(b.getSession(true)).thenReturn(new FakeSession());
        assertNotEquals(Csrf.token(a), Csrf.token(b));
    }

    @Test
    void validHeaderTokenIsAccepted() {
        FakeSession session = new FakeSession();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        String token = Csrf.token(request);
        when(request.getHeader(Csrf.HEADER)).thenReturn(token);
        assertTrue(Csrf.isValid(request));
    }

    @Test
    void validParameterTokenIsAcceptedWhenHeaderMissing() {
        FakeSession session = new FakeSession();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        String token = Csrf.token(request);
        when(request.getHeader(Csrf.HEADER)).thenReturn(null);
        when(request.getParameter(Csrf.PARAM)).thenReturn(token);
        assertTrue(Csrf.isValid(request));
    }

    @Test
    void wrongOrMissingTokenIsRejected() {
        FakeSession session = new FakeSession();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        Csrf.token(request);
        when(request.getHeader(Csrf.HEADER)).thenReturn("totally-wrong");
        when(request.getParameter(Csrf.PARAM)).thenReturn("totally-wrong");
        assertFalse(Csrf.isValid(request));
    }

    @Test
    void noSessionMeansInvalid() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        assertFalse(Csrf.isValid(request));
    }
}
