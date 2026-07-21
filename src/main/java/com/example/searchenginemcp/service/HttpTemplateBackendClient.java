package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.ExecuteTemplateRequest;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryStatus;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
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
    public TemplateExecutionSchema getExecutionSchema(UUID templateId) {
        return restClient.get()
                .uri("/mid/template/{templateId}/execution-schema", templateId)
                .retrieve()
                .body(TemplateExecutionSchema.class);
    }

    @Override
    public QueryExecution executeTemplate(UUID templateId, ExecuteTemplateRequest request) {
        return restClient.post()
                .uri("/mid/template/{templateId}/execute", templateId)
                .body(request)
                .retrieve()
                .body(QueryExecution.class);
    }

    @Override
    public QueryStatus getQueryStatus(UUID executionId) {
        return restClient.get()
                .uri("/mid/query/{executionId}/status", executionId)
                .retrieve()
                .body(QueryStatus.class);
    }

    @Override
    public QueryResult getQueryResult(UUID executionId, int offset, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mid/query/{executionId}/result")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build(executionId))
                .retrieve()
                .body(QueryResult.class);
    }
}
