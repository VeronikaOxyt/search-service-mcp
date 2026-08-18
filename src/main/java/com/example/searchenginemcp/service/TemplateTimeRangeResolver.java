package com.example.searchenginemcp.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemplateTimeRangeResolver {

    private static final DateTimeFormatter BACKEND_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zoneId;

    public TemplateTimeRangeResolver(
            @Value("${search-service.time-zone:Europe/Moscow}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    public void resolve(ObjectNode payload) {
        resolve(payload, ZonedDateTime.now(zoneId));
    }

    void resolve(ObjectNode payload, ZonedDateTime currentTime) {
        JsonNode timeRangeNode = payload.get("timeRange");
        if (timeRangeNode == null || timeRangeNode.isNull()) {
            return;
        }
        if (!(timeRangeNode instanceof ObjectNode timeRange)) {
            throw new IllegalArgumentException("Template timeRange must be a JSON object");
        }

        String type = requiredText(timeRange, "type");
        switch (type) {
            case "range" -> validateAbsoluteRange(timeRange);
            case "mins" -> setRelativeRange(timeRange, currentTime, ChronoUnit.MINUTES);
            case "hours" -> setRelativeRange(timeRange, currentTime, ChronoUnit.HOURS);
            case "days" -> setRelativeRange(timeRange, currentTime, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException(
                    "Unsupported template timeRange type: " + type);
        }

        timeRange.remove("type");
        timeRange.remove("value");
    }

    private void setRelativeRange(
            ObjectNode timeRange,
            ZonedDateTime currentTime,
            ChronoUnit unit) {
        JsonNode valueNode = timeRange.get("value");
        if (valueNode == null || valueNode.isNull() || !valueNode.canConvertToLong()) {
            throw new IllegalArgumentException(
                    "Relative template timeRange does not contain a numeric value");
        }

        long value = valueNode.longValue();
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Relative template timeRange value must be greater than zero");
        }

        ZonedDateTime max = currentTime
                .withZoneSameInstant(zoneId)
                .truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime min = max.minus(value, unit);
        timeRange.put("min", BACKEND_FORMAT.format(min));
        timeRange.put("max", BACKEND_FORMAT.format(max));
    }

    private static void validateAbsoluteRange(ObjectNode timeRange) {
        requiredText(timeRange, "min");
        requiredText(timeRange, "max");
    }

    private static String requiredText(JsonNode object, String fieldName) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull() || value.asString("").isBlank()) {
            throw new IllegalArgumentException(
                    "Template timeRange does not contain required field " + fieldName);
        }
        return value.asString();
    }
}
