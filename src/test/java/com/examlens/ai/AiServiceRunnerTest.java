package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.dao.AiJobDao;
import com.examlens.util.Json;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceRunnerTest {

    /** A scriptable provider so the runner's retry/correction logic can be driven deterministically. */
    private static final class ScriptedAiService implements AiService {
        private final Deque<Object> steps = new ArrayDeque<>();
        private final List<AiRequest> received = new ArrayList<>();
        private boolean configured = true;

        void thenReturn(String raw) {
            steps.add(raw);
        }

        void thenThrow(AiFailureType type) {
            steps.add(new AiException(type, "scripted"));
        }

        void setConfigured(boolean value) {
            this.configured = value;
        }

        @Override public String providerId() { return "scripted"; }
        @Override public String modelId() { return "scripted-1"; }
        @Override public boolean isConfigured() { return configured; }
        @Override public String unavailabilityMessage() { return configured ? null : "not configured"; }

        @Override
        public AiResponse generate(AiRequest request) {
            received.add(request);
            Object next = steps.poll();
            if (next == null) {
                throw new AiException(AiFailureType.UNKNOWN, "no scripted response left");
            }
            if (next instanceof AiException e) {
                throw e;
            }
            return new AiResponse((String) next, providerId(), modelId(), 1L, 0, 0);
        }
    }

    private AppConfig config() {
        // Zero rate limit disables the local counter so the DB is not consulted for it.
        return AppConfig.of(Map.of("AI_RATE_LIMIT_PER_HOUR", "0"));
    }

    private AiValidator<String> titleValidator() {
        return root -> {
            String title = root.path("title").asText("");
            if (title.isBlank()) {
                throw new AiValidationException(List.of("title is required"));
            }
            return title;
        };
    }

    private AiJobDao daoReturning(long jobId) throws Exception {
        AiJobDao dao = Mockito.mock(AiJobDao.class);
        when(dao.create(anyLong(), nullable(Long.class), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(jobId);
        return dao;
    }

    @Test
    void missingProviderConfigurationFailsFast() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.setConfigured(false);
        AiServiceRunner runner = new AiServiceRunner(service, config(), daoReturning(1L));

        AiException error = assertThrows(AiException.class, () -> runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), null));
        assertEquals(AiFailureType.MISSING_API_KEY, error.getFailureType());
    }

    @Test
    void validFirstResponseSucceedsWithNoCorrection() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenReturn("{\"title\":\"Recursion\"}");
        AiJobDao dao = daoReturning(7L);
        AiServiceRunner runner = new AiServiceRunner(service, config(), dao);

        AiOutcome<String> outcome = runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, 2L, Json.object(), titleValidator(), null);

        assertEquals("Recursion", outcome.getData());
        assertEquals(7L, outcome.getJobId());
        assertEquals(0, outcome.getCorrectionsApplied());
        assertEquals("1.1", outcome.getPromptVersion());
        verify(dao).markSucceeded(eq(7L), anyString(), eq(1), eq(0), anyLong());
    }

    @Test
    void invalidFirstResponseTriggersExactlyOneCorrectionRequest() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenReturn("{\"notTitle\":true}");
        service.thenReturn("{\"title\":\"Corrected\"}");
        AiServiceRunner runner = new AiServiceRunner(service, config(), daoReturning(9L));

        AiOutcome<String> outcome = runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), null);

        assertEquals("Corrected", outcome.getData());
        assertEquals(1, outcome.getCorrectionsApplied());
        assertEquals(2, service.received.size(), "the provider must be called twice: initial then correction");
        assertTrue(service.received.get(1).getUserPrompt().contains("REJECTED BY SERVER-SIDE VALIDATION"),
                "the second request must be the correction request");
        assertTrue(service.received.get(1).getUserPrompt().contains("title is required"),
                "the correction request must include the concrete validation error");
    }

    @Test
    void stillInvalidAfterCorrectionFailsHonestly() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenReturn("{}");
        service.thenReturn("{}");
        AiJobDao dao = daoReturning(11L);
        AiServiceRunner runner = new AiServiceRunner(service, config(), dao);

        AiException error = assertThrows(AiException.class, () -> runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), null));
        assertEquals(AiFailureType.SCHEMA_INVALID, error.getFailureType());
        verify(dao).markFailed(eq(11L), anyString(), anyString(), eq(2), eq(1), anyLong());
    }

    @Test
    void retryableProviderFailureIsRetriedThenSucceeds() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenThrow(AiFailureType.TIMEOUT);
        service.thenReturn("{\"title\":\"Recovered\"}");
        AiServiceRunner runner = new AiServiceRunner(service, config(), daoReturning(3L));

        AiOutcome<String> outcome = runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), null);
        assertEquals("Recovered", outcome.getData());
        assertEquals(0, outcome.getCorrectionsApplied());
        assertEquals(2, service.received.size());
    }

    @Test
    void nonRetryableProviderFailureIsNotRetried() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenThrow(AiFailureType.INVALID_API_KEY);
        AiServiceRunner runner = new AiServiceRunner(service, config(), daoReturning(4L));

        AiException error = assertThrows(AiException.class, () -> runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), null));
        assertEquals(AiFailureType.INVALID_API_KEY, error.getFailureType());
        assertEquals(1, service.received.size());
    }

    @Test
    void domainValidationFailureIsSentBackToTheCorrectionRequest() throws Exception {
        ScriptedAiService service = new ScriptedAiService();
        service.thenReturn("{\"title\":\"Valid\"}");
        service.thenReturn("{\"title\":\"Valid\"}");
        AiServiceRunner runner = new AiServiceRunner(service, config(), daoReturning(5L));

        java.util.function.Function<String, List<String>> domainCheck =
                value -> List.of("topic id 99 is not part of this exam");

        AiException error = assertThrows(AiException.class, () -> runner.run(
                AiAction.P13_TOPIC_EXPLANATIONS, 1L, null, Json.object(), titleValidator(), domainCheck));
        assertEquals(AiFailureType.SCHEMA_INVALID, error.getFailureType());
        assertEquals(2, service.received.size());
        assertTrue(service.received.get(1).getUserPrompt().contains("topic id 99"));
    }
}
