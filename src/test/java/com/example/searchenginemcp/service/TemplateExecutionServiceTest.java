package com.example.searchenginemcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.searchenginemcp.dto.template.ExecuteTemplateRequest;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryStatus;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TemplateParameter;
import com.example.searchenginemcp.dto.template.TemplateParameterType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateExecutionServiceTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID EXECUTION_ID = UUID.randomUUID();

    private RecordingTemplateBackendClient backendClient;
    private TemplateExecutionService service;

    @BeforeEach
    void setUp() {
        backendClient = new RecordingTemplateBackendClient();
        service = new TemplateExecutionService(backendClient);
    }

    @Test
    void executesTemplateWhenAllRequiredParametersArePresent() {
        QueryExecution execution = service.execute(
                TEMPLATE_ID,
                3,
                Map.of("username", List.of("ivanov")));

        assertEquals(EXECUTION_ID, execution.executionId());
        assertEquals(3, backendClient.executeRequest.templateVersion());
        assertEquals(
                List.of("ivanov"),
                backendClient.executeRequest.parameters().get("username"));
    }

    @Test
    void rejectsMissingOrBlankRequiredParameter() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        TEMPLATE_ID,
                        3,
                        Map.of("username", List.of(""))));

        assertTrue(error.getMessage().contains("username"));
    }

    @Test
    void rejectsUnknownParameter() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        TEMPLATE_ID,
                        3,
                        Map.of(
                                "username", List.of("ivanov"),
                                "unknown", "value")));

        assertTrue(error.getMessage().contains("unknown"));
    }

    @Test
    void rejectsStaleTemplateVersion() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        TEMPLATE_ID,
                        2,
                        Map.of("username", List.of("ivanov"))));

        assertTrue(error.getMessage().contains("current 3"));
    }

    @Test
    void clampsTemplateAndResultPagination() {
        service.listTemplates(false, 1000, -10);
        service.getResult(EXECUTION_ID, -20, 1000);

        assertEquals(50, backendClient.listRequest.limit());
        assertEquals(0, backendClient.listRequest.offset());
        assertEquals(0, backendClient.resultOffset);
        assertEquals(100, backendClient.resultLimit);
    }

    private static final class RecordingTemplateBackendClient implements TemplateBackendClient {

        private TemplateListRequest listRequest;
        private ExecuteTemplateRequest executeRequest;
        private int resultOffset;
        private int resultLimit;

        @Override
        public TemplateListResponse listTemplates(TemplateListRequest request) {
            listRequest = request;
            return new TemplateListResponse(200, null, 0, List.of());
        }

        @Override
        public TemplateExecutionSchema getExecutionSchema(UUID templateId) {
            return new TemplateExecutionSchema(
                    templateId,
                    3,
                    "User events",
                    "Find events for a user",
                    List.of(
                            new TemplateParameter(
                                    "username",
                                    "User name",
                                    "User whose events should be found",
                                    TemplateParameterType.STRING,
                                    null,
                                    true,
                                    true,
                                    null),
                            new TemplateParameter(
                                    "dateFrom",
                                    "Start date",
                                    "Start date for the search",
                                    TemplateParameterType.DATE,
                                    "yyyy-MM-dd",
                                    false,
                                    false,
                                    "2026-07-01")));
        }

        @Override
        public QueryExecution executeTemplate(UUID templateId, ExecuteTemplateRequest request) {
            executeRequest = request;
            return new QueryExecution(EXECUTION_ID, templateId, "QUEUED", null);
        }

        @Override
        public QueryStatus getQueryStatus(UUID executionId) {
            return new QueryStatus(executionId, "COMPLETED", null, 0L);
        }

        @Override
        public QueryResult getQueryResult(UUID executionId, int offset, int limit) {
            resultOffset = offset;
            resultLimit = limit;
            return new QueryResult(
                    executionId,
                    List.of(),
                    List.of(),
                    0,
                    offset,
                    0,
                    false);
        }
    }
}
