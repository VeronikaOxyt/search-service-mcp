package com.example.searchenginemcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TemplateParameterApplicator {

    private static final int MAX_DEPTH = 20;
    private static final int MAX_NODES = 1_000;

    public JsonNode apply(JsonNode originalTemplate, Map<String, List<String>> suppliedParameters) {
        if (originalTemplate == null || !originalTemplate.isObject()) {
            throw new IllegalArgumentException("Template is empty");
        }

        ObjectNode template = originalTemplate.deepCopy();
        Map<String, List<String>> parameters = suppliedParameters == null
                ? Map.of()
                : suppliedParameters;
        Map<String, ObjectNode> targets = new LinkedHashMap<>();
        TraversalContext context = new TraversalContext();

        JsonNode where = template.get("where");
        if (where != null && !where.isNull()) {
            collectTargets(where, "where", false, 0, context, targets);
        }

        validateParameters(targets, parameters);
        targets.forEach((key, filter) -> filter.set("value", toArray(parameters.get(key))));
        return template;
    }

    private void collectTargets(
            JsonNode node,
            String path,
            boolean parentLocked,
            int depth,
            TraversalContext context,
            Map<String, ObjectNode> targets) {
        if (node == null || node.isNull()) {
            return;
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("Filter at " + path + " is not an object");
        }
        validateTraversal(depth, context);

        boolean locked = parentLocked || node.path("lock").asBoolean(false);
        JsonNode filters = node.get("filters");
        if (filters != null && filters.isArray() && !filters.isEmpty()) {
            for (int index = 0; index < filters.size(); index++) {
                collectTargets(
                        filters.get(index),
                        path + ".filters[" + index + "]",
                        locked,
                        depth + 1,
                        context,
                        targets);
            }
            return;
        }

        if (locked
                || node.path("column").asText("").isBlank()
                || !TemplateParameterExtractor.operatorRequiresValue(node.path("operator").asText())
                || !TemplateParameterExtractor.isEmptyValue(node.get("value"))) {
            return;
        }
        targets.put(path, (ObjectNode) node);
    }

    private static void validateParameters(
            Map<String, ObjectNode> targets,
            Map<String, List<String>> parameters) {
        List<String> unknownKeys = parameters.keySet().stream()
                .filter(key -> !targets.containsKey(key))
                .sorted()
                .toList();
        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown or non-editable template parameters: " + unknownKeys);
        }

        List<String> missingKeys = targets.keySet().stream()
                .filter(key -> !hasValidValue(parameters.get(key)))
                .sorted()
                .toList();
        if (!missingKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required template parameters: " + missingKeys);
        }
    }

    private static boolean hasValidValue(List<String> values) {
        return values != null
                && !values.isEmpty()
                && values.stream().allMatch(value -> value != null && !value.isBlank());
    }

    private static ArrayNode toArray(List<String> values) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        values.forEach(result::add);
        return result;
    }

    private static void validateTraversal(int depth, TraversalContext context) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Template filter tree is deeper than " + MAX_DEPTH);
        }
        context.visitedNodes++;
        if (context.visitedNodes > MAX_NODES) {
            throw new IllegalArgumentException("Template contains more than " + MAX_NODES + " filters");
        }
    }

    private static final class TraversalContext {
        private int visitedNodes;
    }
}
