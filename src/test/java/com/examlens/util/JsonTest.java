package com.examlens.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void extractedJsonIgnoresLeadingProse() {
        JsonNode node = Json.extractJson("Sure, here you go:\n{\"a\":1,\"b\":\"two\"}\nHope this helps!");
        assertEquals(1, node.path("a").asInt());
        assertEquals("two", node.path("b").asText());
    }

    @Test
    void extractedJsonStripsMarkdownFences() {
        JsonNode node = Json.extractJson("```json\n{\"ok\":true}\n```");
        assertTrue(node.path("ok").asBoolean());
    }

    @Test
    void extractedJsonHandlesNestedBracesAndStrings() {
        JsonNode node = Json.extractJson("prefix {\"outer\":{\"inner\":\"a } b\"}} suffix");
        assertEquals("a } b", node.path("outer").path("inner").asText());
    }

    @Test
    void extractedJsonRejectsTextWithoutJson() {
        assertThrows(IllegalArgumentException.class, () -> Json.extractJson("there is no json here"));
    }

    @Test
    void toMapConvertsARecordForElAccess() {
        record Point(int x, int y) { }
        Map<String, Object> map = Json.toMap(new Point(3, 4));
        assertEquals(3, ((Number) map.get("x")).intValue());
        assertEquals(4, ((Number) map.get("y")).intValue());
    }

    @Test
    void readMapListParsesAnArrayOfObjects() {
        List<Map<String, Object>> rows = Json.readMapList("[{\"topic\":\"Recursion\"},{\"topic\":\"Sorting\"}]");
        assertEquals(2, rows.size());
        assertEquals("Recursion", rows.get(0).get("topic"));
        assertEquals("Sorting", rows.get(1).get("topic"));
    }

    @Test
    void readMapListReturnsEmptyListForBlankOrInvalidInput() {
        assertTrue(Json.readMapList(null).isEmpty());
        assertTrue(Json.readMapList("").isEmpty());
        assertTrue(Json.readMapList("not json").isEmpty());
    }

    @Test
    void readStringListParsesJsonArray() {
        assertEquals(List.of("a", "b"), Json.readStringList("[\"a\",\"b\"]"));
    }

    @Test
    void readStringListFallsBackToLineSplitting() {
        List<String> lines = Json.readStringList("first line\nsecond line");
        assertEquals(2, lines.size());
        assertEquals("first line", lines.get(0));
        assertEquals("second line", lines.get(1));
    }

    @Test
    void unknownFieldsAreIgnoredSoAdditiveSchemaChangesDoNotBreakParsing() {
        record Simple(String name) { }
        Simple value = Json.to("{\"name\":\"x\",\"brandNewField\":42}", Simple.class);
        assertEquals("x", value.name());
    }

    @Test
    void stringListHelperSkipsBlanksAndNulls() {
        JsonNode node = Json.read("{\"items\":[\"one\",\"\",null,\"two\"]}");
        assertEquals(List.of("one", "two"), Json.stringList(node, "items"));
        assertFalse(Json.stringList(node, "missing").iterator().hasNext());
    }
}
