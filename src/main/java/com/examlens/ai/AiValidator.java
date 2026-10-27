package com.examlens.ai;

import java.util.List;

/** Domain/structural validation for one AI action's JSON output. */
@FunctionalInterface
public interface AiValidator<T> {

    T validate(com.fasterxml.jackson.databind.JsonNode root) throws AiValidationException;

    /** Convenience factory for validators that only collect error strings. */
    static List<String> errors(String... errors) {
        return List.of(errors);
    }
}
