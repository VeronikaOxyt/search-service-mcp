package com.example.searchenginemcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateTimeRangeResolverTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZonedDateTime NOW = ZonedDateTime.of(
            2026,
            7,
            24,
            10,
            15,
            30,
            0,
            ZoneId.of("Europe/Moscow"));

    private TemplateTimeRangeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TemplateTimeRangeResolver("Europe/Moscow");
    }

    @Test
    void keepsExplicitRangeAndRemovesUiFields() throws Exception {
        ObjectNode payload = payload(
                "range",
                null,
                "2026-07-01 00:00:00",
                "2026-07-02 00:00:00");

        resolver.resolve(payload, NOW);

        assertRange(
                payload,
                "2026-07-01 00:00:00",
                "2026-07-02 00:00:00");
    }

    @Test
    void resolvesMinutesRelativeToCurrentTime() throws Exception {
        ObjectNode payload = payload("mins", 5L, null, null);

        resolver.resolve(payload, NOW);

        assertRange(
                payload,
                "2026-07-24 10:10:30",
                "2026-07-24 10:15:30");
    }

    @Test
    void resolvesHoursRelativeToCurrentTime() throws Exception {
        ObjectNode payload = payload("hours", 2L, null, null);

        resolver.resolve(payload, NOW);

        assertRange(
                payload,
                "2026-07-24 08:15:30",
                "2026-07-24 10:15:30");
    }

    @Test
    void resolvesDaysRelativeToCurrentTime() throws Exception {
        ObjectNode payload = payload("days", 3L, null, null);

        resolver.resolve(payload, NOW);

        assertRange(
                payload,
                "2026-07-21 10:15:30",
                "2026-07-24 10:15:30");
    }

    private static ObjectNode payload(
            String type,
            Long value,
            String min,
            String max) throws Exception {
        ObjectNode payload = (ObjectNode) OBJECT_MAPPER.readTree("""
                {
                  "timeRange": {
                    "column": "B_ReceiptTime"
                  }
                }
                """);
        ObjectNode timeRange = (ObjectNode) payload.get("timeRange");
        timeRange.put("type", type);
        if (value == null) {
            timeRange.putNull("value");
        } else {
            timeRange.put("value", value);
        }
        if (min != null) {
            timeRange.put("min", min);
        }
        if (max != null) {
            timeRange.put("max", max);
        }
        return payload;
    }

    private static void assertRange(
            ObjectNode payload,
            String expectedMin,
            String expectedMax) {
        assertEquals(expectedMin, payload.at("/timeRange/min").asString());
        assertEquals(expectedMax, payload.at("/timeRange/max").asString());
        assertTrue(payload.at("/timeRange/type").isMissingNode());
        assertTrue(payload.at("/timeRange/value").isMissingNode());
    }
}
