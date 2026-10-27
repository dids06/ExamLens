package com.examlens.ai;

import java.util.ArrayList;
import java.util.List;

/** Thrown when AI JSON output fails structural or domain validation. */
public class AiValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public AiValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public AiValidationException(String error) {
        this(List.of(error));
    }

    public List<String> getErrors() {
        return errors;
    }

    public static AiValidationException of(String... errors) {
        return new AiValidationException(new ArrayList<>(List.of(errors)));
    }
}
