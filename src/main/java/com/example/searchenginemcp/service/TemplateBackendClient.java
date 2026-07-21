package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.template.ExecuteTemplateRequest;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryStatus;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import java.util.UUID;

public interface TemplateBackendClient {

    TemplateListResponse listTemplates(TemplateListRequest request);

    TemplateExecutionSchema getExecutionSchema(UUID templateId);

    QueryExecution executeTemplate(UUID templateId, ExecuteTemplateRequest request);

    QueryStatus getQueryStatus(UUID executionId);

    QueryResult getQueryResult(UUID executionId, int offset, int limit);
}
