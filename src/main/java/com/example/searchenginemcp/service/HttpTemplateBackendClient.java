package com.example.searchenginemcp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.searchenginemcp.dto.template.BackendTemplateResponse;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryResultMeta;
import com.example.searchenginemcp.dto.template.QueryResultRequest;
import com.example.searchenginemcp.dto.template.QueryResultState;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.ResultColumn;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TopologyInfoTableResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HttpTemplateBackendClient implements TemplateBackendClient {

    private final RestClient restClient;

    public HttpTemplateBackendClient(RestClient searchServiceRestClient) {
        this.restClient = searchServiceRestClient;
    }

    @Override
    public TemplateListResponse listTemplates(TemplateListRequest request) {
        return restClient.post()
                .uri("/mid/template/list")
                .body(request)
                .retrieve()
                .body(TemplateListResponse.class);
    }

    @Override
    public BackendTemplateResponse getTemplate(UUID templateId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mid/template")
                        .queryParam("id", templateId)
                        .build())
                .retrieve()
                .body(BackendTemplateResponse.class);
    }

    @Override
    public TopologyInfoTableResponse getTableStructure(
            String sourceName,
            String schemaName,
            String tableName) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mid/query/topology/structureTable")
                        .queryParam("schemaName", schemaName)
                        .queryParam("tableName", tableName)
                        .queryParam("sourceName", sourceName)
                        .build())
                .retrieve()
                .body(TopologyInfoTableResponse.class);
    }

    @Override
    public QueryExecution executeTemplate(QueryType queryType, JsonNode executionPayload) {
        String path = switch (queryType) {
            case QUERY -> "/mid/query/executeQuery";
            case CROSS -> "/mid/query/executeCrossQuery";
        };

        return restClient.post()
                .uri(path)
                .body(executionPayload)
                .retrieve()
                .body(QueryExecution.class);
    }

    @Override
    public QueryResult getQueryResult(UUID resultId, QueryType queryType, int offset, int limit) {
        QueryResultRequest request = new QueryResultRequest(limit, offset, resultId);
        String path = switch (queryType) {
            case QUERY -> "/mid/query/result";
            case CROSS -> "/mid/query/crossResult";
        };

        return restClient.post()
                .uri(path)
                .body(request)
                .exchange((httpRequest, response) -> {
                    int httpStatus = response.getStatusCode().value();
                    if (httpStatus != 200 && httpStatus != 425 && httpStatus != 426) {
                        String responseBody = response.bodyTo(String.class);
                        throw new IllegalStateException(
                                "Query result backend returned HTTP %d: %s"
                                        .formatted(httpStatus, responseBody));
                    }

                    BackendResultResponse body = queryType == QueryType.QUERY
                            ? response.bodyTo(QueryResponse.class)
                            : response.bodyTo(CrossQueryResponse.class);
                    if (body == null) {
                        throw new IllegalStateException("Query result backend returned an empty response");
                    }
                    return toQueryResult(resultId, queryType, offset, httpStatus, body);
                });
    }

    private static QueryResult toQueryResult(
            UUID resultId,
            QueryType queryType,
            int offset,
            int httpStatus,
            BackendResultResponse response) {
        BackendResultPayload payload = response.payload();
        BackendMetaInfo backendMeta = payload == null ? null : payload.metaInfo();
        QueryResultMeta meta = toMeta(backendMeta);
        List<Map<String, Object>> rows = payload == null || payload.resultData() == null
                ? List.of()
                : payload.resultData();
        boolean ready = httpStatus == 200;
        long totalRows = meta == null ? 0 : meta.count();

        return new QueryResult(
                resultId,
                queryType,
                ready ? QueryResultState.READY : QueryResultState.PENDING,
                httpStatus,
                response.message(),
                meta,
                ready ? rows : List.of(),
                offset,
                ready ? rows.size() : 0,
                ready && totalRows > (long) offset + rows.size());
    }

    private static QueryResultMeta toMeta(BackendMetaInfo meta) {
        if (meta == null) {
            return null;
        }
        return new QueryResultMeta(
                meta.baseColumns() == null ? List.of() : meta.baseColumns(),
                meta.sourceName(),
                meta.status(),
                meta.count() == null ? 0 : meta.count(),
                meta.progress(),
                meta.completedCount(),
                meta.failedCount(),
                meta.allCount(),
                meta.columns() == null ? List.of() : meta.columns());
    }

    private interface BackendResultResponse {
        String message();

        BackendResultPayload payload();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QueryResponse(
            int status,
            String timestamp,
            String message,
            BackendResultPayload result) implements BackendResultResponse {

        @Override
        public BackendResultPayload payload() {
            return result;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CrossQueryResponse(
            int status,
            String timestamp,
            String message,
            BackendResultPayload crossResult) implements BackendResultResponse {

        @Override
        public BackendResultPayload payload() {
            return crossResult;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BackendResultPayload(
            BackendMetaInfo metaInfo,
            List<Map<String, Object>> resultData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BackendMetaInfo(
            List<String> baseColumns,
            String sourceName,
            String status,
            Long count,
            Integer progress,
            Integer completedCount,
            Integer failedCount,
            Integer allCount,
            List<ResultColumn> columns) {
    }
}
