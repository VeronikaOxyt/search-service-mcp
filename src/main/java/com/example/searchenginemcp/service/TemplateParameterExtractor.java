package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.TemplateParameter;
import com.example.searchenginemcp.dto.template.TemplateParameterType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TemplateParameterExtractor {

    private static final int MAX_DEPTH = 20;
    private static final int MAX_NODES = 1_000;
    private static final Set<String> OPERATORS_WITHOUT_VALUE = Set.of(
            "is n/a",
            "is not n/a",
            "is null",
            "is not null");

    public List<TemplateParameter> extract(JsonNode template) {
        if (template == null || template.isNull()) {
            throw new IllegalArgumentException("Template is empty");
        }

        JsonNode where = template.get("where");
        if (where == null || where.isNull()) {
            return List.of();
        }

        List<TemplateParameter> result = new ArrayList<>();
        TraversalContext context = new TraversalContext();
        walk(where, "where", false, 0, context, result);
        return List.copyOf(result);
    }

    private void walk(
            JsonNode node,
            String path,
            boolean parentLocked,
            int depth,
            TraversalContext context,
            List<TemplateParameter> result) {
        if (node == null || node.isNull()) {
            return;
        }
        validateTraversal(depth, context);

        boolean locked = parentLocked || node.path("lock").asBoolean(false);
        JsonNode filters = node.get("filters");
        if (filters != null && filters.isArray() && !filters.isEmpty()) {
            for (int index = 0; index < filters.size(); index++) {
                walk(
                        filters.get(index),
                        path + ".filters[" + index + "]",
                        locked,
                        depth + 1,
                        context,
                        result);
            }
            return;
        }

        if (locked || !isLeaf(node) || !operatorRequiresValue(node.path("operator").asText())) {
            return;
        }
        if (!isEmptyValue(node.get("value"))) {
            return;
        }

        String column = node.path("column").asText();
        result.add(new TemplateParameter(
                path,
                column,
                "Enter a value for column %s and operator %s"
                        .formatted(column, node.path("operator").asText()),
                TemplateParameterType.STRING,
                null,
                true,
                true,
                null));
    }

    private static boolean isLeaf(JsonNode node) {
        return !node.path("column").asText("").isBlank()
                && !node.path("operator").asText("").isBlank();
    }

    static boolean isEmptyValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return true;
        }
        if (!value.isArray()) {
            return value.asText("").isBlank();
        }
        if (value.isEmpty()) {
            return true;
        }
        for (JsonNode element : value) {
            if (element != null && !element.isNull() && !element.asText("").isBlank()) {
                return false;
            }
        }
        return true;
    }

    static boolean operatorRequiresValue(String operator) {
        if (operator == null || operator.isBlank()) {
            return false;
        }
        return !OPERATORS_WITHOUT_VALUE.contains(
                operator.trim().toLowerCase(Locale.ROOT));
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
