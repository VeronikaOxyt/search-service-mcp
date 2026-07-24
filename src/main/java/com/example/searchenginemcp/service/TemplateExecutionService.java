package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.BackendTemplateResponse;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TemplateListResult;
import com.example.searchenginemcp.dto.template.TopologyInfoTableResponse;
import com.example.searchenginemcp.dto.template.TopologyTableColumn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TemplateExecutionService {

    private static final int DEFAULT_TEMPLATE_LIMIT = 20;
    private static final int MAX_TEMPLATE_LIMIT = 50;
    private static final int DEFAULT_RESULT_LIMIT = 20;
    private static final int MAX_RESULT_LIMIT = 100;

    private final TemplateBackendClient backendClient;
    private final TemplateParameterExtractor parameterExtractor;
    private final TemplateParameterApplicator parameterApplicator;
    private final TemplateTimeRangeResolver timeRangeResolver;

    public TemplateExecutionService(
            TemplateBackendClient backendClient,
            TemplateParameterExtractor parameterExtractor,
            TemplateParameterApplicator parameterApplicator,
            TemplateTimeRangeResolver timeRangeResolver) {
        this.backendClient = backendClient;
        this.parameterExtractor = parameterExtractor;
        this.parameterApplicator = parameterApplicator;
        this.timeRangeResolver = timeRangeResolver;
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
        BackendTemplateResponse response = getTemplate(templateId);
        JsonNode template = requireResponse(
                response.template(),
                "Template backend returned an empty template");
        QueryType queryType = getQueryType(response);

        return new TemplateExecutionSchema(
                templateId,
                textOrNull(template.get("name")),
                null,
                queryType,
                parameterExtractor.extract(template));
    }

    public QueryExecution execute(
            UUID templateId,
            Map<String, List<String>> suppliedParameters) {
        BackendTemplateResponse response = getTemplate(templateId);
        JsonNode filledTemplate = parameterApplicator.apply(
                response.template(),
                suppliedParameters);
        QueryType queryType = getQueryType(response);
        JsonNode executionPayload = createExecutionPayload(filledTemplate, templateId);
        timeRangeResolver.resolve((ObjectNode) executionPayload);
        if (queryType == QueryType.QUERY) {
            addBaseColumns((ObjectNode) executionPayload);
        }

        QueryExecution execution = requireResponse(
                backendClient.executeTemplate(queryType, executionPayload),
                "Template execution backend returned an empty response");
        return new QueryExecution(
                execution.resultId(),
                execution.templateId() == null ? templateId : execution.templateId(),
                queryType,
                execution.status(),
                execution.message());
    }

    private static JsonNode createExecutionPayload(
            JsonNode filledTemplate,
            UUID templateId) {
        if (!(filledTemplate instanceof ObjectNode payload)) {
            throw new IllegalArgumentException("Template must be a JSON object");
        }

        String templateName = textOrNull(payload.get("name"));
        payload.put(
                "name",
                templateName == null || templateName.isBlank()
                        ? "Запрос по шаблону"
                        : "Запрос по шаблону: " + templateName);
        payload.put("templateId", templateId.toString());
        payload.put("rqUid", UUID.randomUUID().toString());
        return payload;
    }

    private void addBaseColumns(ObjectNode payload) {
        String sourceName = requiredText(payload, "sourceName");
        JsonNode table = payload.get("table");
        if (table == null || !table.isObject()) {
            throw new IllegalArgumentException(
                    "Regular query template does not contain table");
        }

        String schemaName = requiredText(table, "schema");
        String tableName = requiredText(table, "tableName");
        TopologyInfoTableResponse response = requireResponse(
                backendClient.getTableStructure(sourceName, schemaName, tableName),
                "Table structure backend returned an empty response");
        if (response.tableInfo() == null) {
            throw new IllegalStateException(
                    "Table structure backend returned an empty tableInfo");
        }

        List<TopologyTableColumn> columns = response.tableInfo().columns() == null
                ? List.of()
                : response.tableInfo().columns();
        ArrayNode baseColumns = payload.putArray("baseColumns");
        columns.stream()
                .filter(TopologyTableColumn::isBaseColumn)
                .map(TopologyTableColumn::columnName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(baseColumns::add);
    }

    private static String requiredText(JsonNode object, String fieldName) {
        String value = textOrNull(object.get(fieldName));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Template does not contain required field " + fieldName);
        }
        return value;
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

    private BackendTemplateResponse getTemplate(UUID templateId) {
        return requireResponse(
                backendClient.getTemplate(templateId),
                "Template backend returned an empty response");
    }

    private static QueryType getQueryType(BackendTemplateResponse response) {
        return response.metaInfo() != null
                        && Boolean.TRUE.equals(response.metaInfo().isCross())
                ? QueryType.CROSS
                : QueryType.QUERY;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static <T> T requireResponse(T response, String message) {
        if (response == null) {
            throw new IllegalStateException(message);
        }
        return response;
    }
}
