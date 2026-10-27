package com.examlens.service;

import com.examlens.model.DocumentBlock;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;

/**
 * Builds the JSON inputs handed to each AI action. Field names are part of the AI
 * contract and are kept identical to the schemas in {@code PromptLibrary} and the
 * deterministic mock provider so mock and live providers receive the same shape.
 */
public final class AiInputs {

    private AiInputs() {
    }

    public static ObjectNode object() {
        return Json.object();
    }

    public static ArrayNode strings(Collection<String> values) {
        ArrayNode array = Json.array();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    array.add(v);
                }
            }
        }
        return array;
    }

    public static ArrayNode blocks(List<DocumentBlock> blocks) {
        ArrayNode array = Json.array();
        for (DocumentBlock b : blocks) {
            ObjectNode node = Json.object();
            node.put("blockKey", b.getBlockKey());
            if (b.getPageNumber() != null) {
                node.put("page", b.getPageNumber());
            }
            if (b.getHeading() != null) {
                node.put("heading", b.getHeading());
            }
            node.put("text", b.getBlockText() == null ? "" : b.getBlockText());
            node.put("method", b.getExtractionMethod());
            if (b.getConfidence() != null) {
                node.put("confidence", b.getConfidence());
            }
            node.put("uncertain", b.isUncertain());
            array.add(node);
        }
        return array;
    }

    public static String rawText(List<DocumentBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (DocumentBlock b : blocks) {
            if (b.getBlockText() != null) {
                sb.append(b.getBlockText()).append('\n');
            }
        }
        return sb.toString();
    }

    public static ObjectNode topicNode(Topic t) {
        ObjectNode node = Json.object();
        node.put("topicId", String.valueOf(t.getId()));
        node.put("title", t.getTitle() == null ? "" : t.getTitle());
        if (t.getUnitName() != null) {
            node.put("unitName", t.getUnitName());
        }
        if (t.getImportance() != null) {
            node.put("importance", t.getImportance());
        }
        if (t.getDifficulty() != null) {
            node.put("difficulty", t.getDifficulty());
        }
        node.put("estimatedMinutes", t.getEstimatedMinutes() == null ? 60 : t.getEstimatedMinutes());
        node.set("subtopics", strings(t.getSubtopicList()));
        node.set("learningOutcomes", strings(t.getOutcomeList()));
        return node;
    }

    public static ArrayNode topics(Collection<Topic> topics) {
        ArrayNode array = Json.array();
        for (Topic t : topics) {
            array.add(topicNode(t));
        }
        return array;
    }

    public static ArrayNode availability(List<AvailabilityDay> days) {
        ArrayNode array = Json.array();
        for (AvailabilityDay d : days) {
            ObjectNode node = Json.object();
            node.put("date", d.date().toString());
            node.put("minutes", d.minutes());
            if (d.label() != null) {
                node.put("label", d.label());
            }
            array.add(node);
        }
        return array;
    }

    public static ArrayNode availabilityWithTotals(Collection<AvailabilityDay> days) {
        return availability(List.copyOf(days));
    }
}
