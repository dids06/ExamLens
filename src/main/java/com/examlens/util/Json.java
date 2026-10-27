package com.examlens.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Jackson helpers. Unknown AI fields are ignored so additive model changes do not break parsing. */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode array() {
        return MAPPER.createArrayNode();
    }

    /**
     * Converts any object (including records and public-field DTOs) into nested maps/lists so JSP EL
     * can read it as bean-style properties without requiring getters on the source type.
     */
    public static Map<String, Object> toMap(Object value) {
        if (value == null) {
            return new java.util.LinkedHashMap<>();
        }
        return MAPPER.convertValue(value,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {
                });
    }

    public static List<Map<String, Object>> toMaps(List<?> values) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (values != null) {
            for (Object value : values) {
                out.add(toMap(value));
            }
        }
        return out;
    }

    /** Parses a stored JSON array of objects into maps for JSP EL. Returns an empty list on failure. */
    public static List<Map<String, Object>> readMapList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Object> raw = MAPPER.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
            });
            return toMaps(raw);
        } catch (RuntimeException | JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /** Parses a stored JSON array of strings, falling back to newline splitting. */
    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return json.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
        }
    }

    public static String write(Object value) {        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize value", e);
        }
    }

    public static String writePretty(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize value", e);
        }
    }

    public static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON", e);
        }
    }

    public static <T> T to(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot map JSON to " + type.getSimpleName(), e);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> type) {
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot map JSON to " + type.getSimpleName(), e);
        }
    }

    public static List<String> stringList(JsonNode node, String field) {
        List<String> out = new ArrayList<>();
        JsonNode arr = node == null ? null : node.get(field);
        if (arr != null && arr.isArray()) {
            for (JsonNode item : arr) {
                if (item != null && !item.isNull()) {
                    String s = item.asText();
                    if (s != null && !s.isBlank()) {
                        out.add(s.trim());
                    }
                }
            }
        }
        return out;
    }

    public static List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        return List.of(String.valueOf(value));
    }

    public static JsonNode map(Map<String, ?> map) {
        return MAPPER.valueToTree(map);
    }

    /** Extracts the first JSON object/array from a model response that may contain prose or fences. */
    public static JsonNode extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Empty AI response");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException ignored) {
            // fall through to brace scanning
        }
        int start = -1;
        char open = 0;
        char close = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                open = c;
                close = c == '{' ? '}' : ']';
                break;
            }
        }
        if (start < 0) {
            throw new IllegalArgumentException("AI response did not contain JSON");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    String candidate = text.substring(start, i + 1);
                    return read(candidate);
                }
            }
        }
        throw new IllegalArgumentException("AI response contained unbalanced JSON");
    }
}
