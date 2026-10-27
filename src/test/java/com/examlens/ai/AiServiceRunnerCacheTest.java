package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.dao.AiJobDao;
import com.examlens.util.Json;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

class AiServiceRunnerCacheTest {

    private static final class ScriptedAiService implements AiService {
        private final Deque<Object> steps = new ArrayDeque<>();
        private int calls = 0;
        void thenReturn(String raw) { steps.add(raw); }
        void thenThrow(AiException e) { steps.add(e); }
        int calls() { return calls; }
        @Override public String providerId() { return "scripted"; }
        @Override public String modelId() { return "scripted-1"; }
        @Override public boolean isConfigured() { return true; }
        @Override public String unavailabilityMessage() { return null; }
        @Override public AiResponse generate(AiRequest request) {
            calls++;
            Object next = steps.poll();
            if (next instanceof AiException e) throw e;
            return new AiResponse(String.valueOf(next), "scripted", "scripted-1", 5L, 10, 20);
        }
    }

    private AiJobDao mockDao() throws Exception {
        AiJobDao dao = Mockito.mock(AiJobDao.class);
        when(dao.create(anyLong(), nullable(Long.class), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(1L);
        when(dao.countSince(anyLong(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(0);
        return dao;
    }

    private AppConfig cfg() {
        return AppConfig.of(Map.of("AI_PROVIDER", "mock", "AI_RATE_LIMIT_PER_HOUR", "0"));
    }

    @Test
    void identicalSecondCallIsCacheHit() throws Exception {
        ScriptedAiService provider = new ScriptedAiService();
        provider.thenReturn("{\"ok\":true}");
        AiJobDao dao = mockDao();
        AiServiceRunner runner = new AiServiceRunner(provider, cfg(), dao);

        JsonNode input = Json.read("{\"q\":\"hi\"}");
        AiOutcome<JsonNode> first = runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, input, (root) -> root, null);
        AiOutcome<JsonNode> second = runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, input, (root) -> root, null);

        assertEquals(false, first.isCacheHit());
        assertEquals(true, second.isCacheHit());
        assertEquals(1, provider.calls());
    }

    @Test
    void cacheIsolatedByUser() throws Exception {
        ScriptedAiService provider = new ScriptedAiService();
        provider.thenReturn("{\"ok\":true}");
        provider.thenReturn("{\"ok\":true}");
        AiJobDao dao = mockDao();
        AiServiceRunner runner = new AiServiceRunner(provider, cfg(), dao);

        JsonNode input = Json.read("{\"q\":\"hi\"}");
        runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, input, (root) -> root, null);
        runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 2L, null, input, (root) -> root, null);

        assertEquals(2, provider.calls(), "different users must not share cache");
    }

    @Test
    void nonTransientClientErrorIsNotRetried() throws Exception {
        ScriptedAiService provider = new ScriptedAiService();
        provider.thenThrow(new AiException(AiFailureType.PROVIDER_ERROR, "HTTP 400: bad request"));
        AiJobDao dao = mockDao();
        AiServiceRunner runner = new AiServiceRunner(provider, cfg(), dao);

        JsonNode input = Json.read("{\"q\":\"hi\"}");
        try {
            runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, input, (root) -> root, null);
        } catch (AiException ignored) { }
        assertEquals(1, provider.calls(), "4xx PROVIDER_ERROR must not be retried");
    }

    @Test
    void transientErrorIsRetried() throws Exception {
        ScriptedAiService provider = new ScriptedAiService();
        provider.thenThrow(new AiException(AiFailureType.RATE_LIMITED, "scripted"));
        provider.thenReturn("{\"ok\":true}");
        AiJobDao dao = mockDao();
        AiServiceRunner runner = new AiServiceRunner(provider, cfg(), dao);

        JsonNode input = Json.read("{\"q\":\"hi\"}");
        AiOutcome<?> out = runner.run(AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, input, (root) -> root, null);
        assertEquals(2, provider.calls(), "transient error should be retried once");
    }
}
