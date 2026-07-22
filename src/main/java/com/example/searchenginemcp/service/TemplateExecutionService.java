package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.ExecuteTemplateRequest;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TemplateListResult;
import com.example.searchenginemcp.dto.template.TemplateParameter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TemplateExecutionService {

    private static final int DEFAULT_TEMPLATE_LIMIT = 20;
    private static final int MAX_TEMPLATE_LIMIT = 50;
    private static final int DEFAULT_RESULT_LIMIT = 20;
    private static final int MAX_RESULT_LIMIT = 100;

    private final TemplateBackendClient backendClient;

    public TemplateExecutionService(TemplateBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    public TemplateListResult listTemplates(
            boolean personal,
            Integer requestedLimit,
            Integer requestedOffset) {
        int limit = clamp(requestedLimit, DEFAULT_TEMPLATE_LIMIT, 1, MAX_TEMPLATE_LIMIT);
        int offset = Math.max(requestedOffset == null ? 0 : requestedOffset, 0);

        TemplateListResponse response = requireResponse(
                backendClient.listTemplates(TemplateListRequest.of(personal, limit, offset)),
                "Template list backend returned an empty response");

        return new TemplateListResult(
                response.templateCount(),
                response.templateList() == null ? List.of() : response.templateList());
    }

    public TemplateExecutionSchema getExecutionSchema(UUID templateId) {
        return requireResponse(
                backendClient.getExecutionSchema(templateId),
                "Template execution schema backend returned an empty response");
    }

    public QueryExecution execute(
            UUID templateId,
            long templateVersion,
            Map<String, Object> suppliedParameters) {
        TemplateExecutionSchema schema = getExecutionSchema(templateId);
        Map<String, Object> parameters = suppliedParameters == null
                ? Map.of()
                : suppliedParameters;

        if (schema.version() != templateVersion) {
            throw new IllegalArgumentException(
                    "Template version changed: requested %d, current %d"
                            .formatted(templateVersion, schema.version()));
        }

        List<TemplateParameter> definitions = schema.parameters() == null
                ? List.of()
                : schema.parameters();
        Set<String> allowedKeys = definitions.stream()
                .map(TemplateParameter::key)
                .collect(Collectors.toSet());

        List<String> unknownKeys = parameters.keySet().stream()
                .filter(key -> !allowedKeys.contains(key))
                .sorted()
                .toList();
        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown template parameters: " + unknownKeys);
        }

        List<String> missingKeys = definitions.stream()
                .filter(TemplateParameter::required)
                .filter(parameter -> !hasValue(parameters.get(parameter.key())))
                .map(TemplateParameter::key)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!missingKeys.isEmpty()) {
            throw new IllegalArgumentException("Missing required template parameters: " + missingKeys);
        }

        QueryExecution execution = requireResponse(
                backendClient.executeTemplate(
                        templateId,
                        new ExecuteTemplateRequest(templateVersion, parameters)),
                "Template execution backend returned an empty response");
        QueryType queryType = requireResponse(
                schema.queryType(),
                "Template execution schema does not contain queryType");
        return new QueryExecution(
                execution.resultId(),
                execution.templateId() == null ? templateId : execution.templateId(),
                queryType,
                execution.status(),
                execution.message());
    }

    public QueryResult getResult(
            UUID resultId,
            QueryType queryType,
            Integer requestedOffset,
            Integer requestedLimit) {
        int offset = Math.max(requestedOffset == null ? 0 : requestedOffset, 0);
        int limit = clamp(requestedLimit, DEFAULT_RESULT_LIMIT, 1, MAX_RESULT_LIMIT);

        return requireResponse(
                backendClient.getQueryResult(resultId, queryType, offset, limit),
                "Query result backend returned an empty response");
    }

    private static int clamp(Integer requested, int defaultValue, int min, int max) {
        int value = requested == null ? defaultValue : requested;
        return Math.min(Math.max(value, min), max);
    }

    private static boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence text) {
            return !text.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty() && collection.stream().anyMatch(TemplateExecutionService::hasValue);
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                if (hasValue(Array.get(value, index))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static <T> T requireResponse(T response, String message) {
        if (response == null) {
            throw new IllegalStateException(message);
        }
        return response;
    }
}
