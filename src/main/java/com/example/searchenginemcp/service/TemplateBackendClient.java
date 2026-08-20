package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.BackendTemplateResponse;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TopologyInfoTableResponse;
import tools.jackson.databind.JsonNode;
import java.util.UUID;

public interface TemplateBackendClient {

    TemplateListResponse listTemplates(TemplateListRequest request, String authorization);

    BackendTemplateResponse getTemplate(UUID templateId, String authorization);

    TopologyInfoTableResponse getTableStructure(
            String sourceName,
            String schemaName,
            String tableName,
            String authorization);

    QueryExecution executeTemplate(
            QueryType queryType,
            JsonNode executionPayload,
            String authorization);

    QueryResult getQueryResult(
            UUID resultId,
            QueryType queryType,
            int offset,
            int limit,
            String authorization);
}
