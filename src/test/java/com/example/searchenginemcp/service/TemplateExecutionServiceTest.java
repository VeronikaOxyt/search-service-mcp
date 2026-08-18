package com.example.searchenginemcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.searchenginemcp.dto.template.BackendTemplateResponse;
import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryResultMeta;
import com.example.searchenginemcp.dto.template.QueryResultState;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListRequest;
import com.example.searchenginemcp.dto.template.TemplateListResponse;
import com.example.searchenginemcp.dto.template.TemplateMetaInfo;
import com.example.searchenginemcp.dto.template.TopologyInfoTableResponse;
import com.example.searchenginemcp.dto.template.TopologyTable;
import com.example.searchenginemcp.dto.template.TopologyTableColumn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateExecutionServiceTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecordingTemplateBackendClient backendClient;
    private TemplateExecutionService service;

    @BeforeEach
    void setUp() {
        backendClient = new RecordingTemplateBackendClient();
        service = new TemplateExecutionService(
                backendClient,
                new TemplateParameterExtractor(),
                new TemplateParameterApplicator(),
                new TemplateTimeRangeResolver("Europe/Moscow"));
    }

    @Test
    void extractsEmptyUnlockedFiltersRecursively() {
        TemplateExecutionSchema schema = service.getExecutionSchema(TEMPLATE_ID);

        assertEquals("User events", schema.name());
        assertEquals(QueryType.QUERY, schema.queryType());
        assertEquals(
                List.of("where.filters[0]", "where.filters[1].filters[0]"),
                schema.parameters().stream().map(parameter -> parameter.key()).toList());
    }

    @Test
    void executesTemplateWhenAllEmptyFiltersAreFilled() {
        QueryExecution execution = service.execute(
                TEMPLATE_ID,
                Map.of(
                        "where.filters[0]", List.of("ivanov"),
                        "where.filters[1].filters[0]", List.of("10.0.0.1")));

        assertEquals(RESULT_ID, execution.resultId());
        assertEquals(QueryType.QUERY, execution.queryType());
        assertEquals(
                "ivanov",
                backendClient.filledTemplate
                        .at("/where/filters/0/value/0")
                        .asString());
        assertEquals(
                "10.0.0.1",
                backendClient.filledTemplate
                        .at("/where/filters/1/filters/0/value/0")
                        .asString());
        assertEquals(
                "123",
                backendClient.filledTemplate
                        .at("/where/filters/1/filters/1/value/0")
                        .asString());
        assertEquals(
                "Запрос по шаблону: User events",
                backendClient.filledTemplate.path("name").asString());
        assertEquals(
                TEMPLATE_ID.toString(),
                backendClient.filledTemplate.path("templateId").asString());
        UUID.fromString(backendClient.filledTemplate.path("rqUid").asString());
        assertEquals(
                "2026-07-23 16:43:42",
                backendClient.filledTemplate.at("/timeRange/min").asString());
        assertEquals(
                "2026-07-23 16:48:42",
                backendClient.filledTemplate.at("/timeRange/max").asString());
        assertTrue(backendClient.filledTemplate.at("/timeRange/type").isMissingNode());
        assertTrue(backendClient.filledTemplate.at("/timeRange/value").isMissingNode());
        assertEquals(
                "SourceIP",
                backendClient.filledTemplate.path("baseColumns").get(0).asString());
        assertEquals(
                "Username",
                backendClient.filledTemplate.path("baseColumns").get(1).asString());
        assertEquals("datastore_clickhouse", backendClient.structureSourceName);
        assertEquals("log_armatm_src_distr", backendClient.structureSchemaName);
        assertEquals("parsed", backendClient.structureTableName);
        assertEquals(QueryType.QUERY, backendClient.executeQueryType);
    }

    @Test
    void doesNotLoadBaseColumnsForCrossQuery() {
        backendClient.cross = true;

        QueryExecution execution = service.execute(
                TEMPLATE_ID,
                Map.of(
                        "where.filters[0]", List.of("ivanov"),
                        "where.filters[1].filters[0]", List.of("10.0.0.1")));

        assertEquals(QueryType.CROSS, execution.queryType());
        assertEquals(QueryType.CROSS, backendClient.executeQueryType);
        assertEquals(0, backendClient.structureRequests);
        assertTrue(backendClient.filledTemplate.path("baseColumns").isMissingNode());
    }

    @Test
    void rejectsMissingOrBlankRequiredParameter() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        TEMPLATE_ID,
                        Map.of(
                                "where.filters[0]", List.of(""),
                                "where.filters[1].filters[0]", List.of("10.0.0.1"))));

        assertTrue(error.getMessage().contains("where.filters[0]"));
    }

    @Test
    void rejectsUnknownParameter() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        TEMPLATE_ID,
                        Map.of("unknown", List.of("value"))));

        assertTrue(error.getMessage().contains("unknown"));
    }

    @Test
    void clampsTemplateAndResultPagination() {
        service.listTemplates(false, 1000, -10);
        service.getResult(RESULT_ID, QueryType.CROSS, -20, 1000);

        assertEquals(50, backendClient.listRequest.limit());
        assertEquals(0, backendClient.listRequest.offset());
        assertEquals(0, backendClient.resultOffset);
        assertEquals(100, backendClient.resultLimit);
        assertEquals(QueryType.CROSS, backendClient.resultQueryType);
    }

    private static JsonNode template() {
        try {
            return OBJECT_MAPPER.readTree("""
                    {
                      "name": "User events",
                      "sourceName": "datastore_clickhouse",
                      "table": {
                        "schema": "log_armatm_src_distr",
                        "tableName": "parsed"
                      },
                      "timeRange": {
                        "column": "B_ReceiptTime",
                        "min": "2026-07-23 16:43:42",
                        "max": "2026-07-23 16:48:42",
                        "type": "range",
                        "value": null
                      },
                      "where": {
                        "logicOperator": "AND",
                        "filters": [
                          {
                            "column": "Username",
                            "operator": "Equals",
                            "value": [""],
                            "lock": false
                          },
                          {
                            "logicOperator": "OR",
                            "filters": [
                              {
                                "column": "SourceIP",
                                "operator": "Equals",
                                "value": [],
                                "lock": false
                              },
                              {
                                "column": "VendorEventId",
                                "operator": "Equals",
                                "value": ["123"],
                                "lock": false
                              },
                              {
                                "column": "VendorEventId",
                                "operator": "is N/A",
                                "value": [],
                                "lock": false
                              }
                            ]
                          }
                        ]
                      }
                    }
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class RecordingTemplateBackendClient implements TemplateBackendClient {

        private TemplateListRequest listRequest;
        private JsonNode filledTemplate;
        private boolean cross;
        private int structureRequests;
        private String structureSourceName;
        private String structureSchemaName;
        private String structureTableName;
        private QueryType executeQueryType;
        private int resultOffset;
        private int resultLimit;
        private QueryType resultQueryType;

        @Override
        public TemplateListResponse listTemplates(TemplateListRequest request) {
            listRequest = request;
            return new TemplateListResponse(200, null, 0, List.of());
        }

        @Override
        public BackendTemplateResponse getTemplate(UUID templateId) {
            return new BackendTemplateResponse(
                    template(),
                    new TemplateMetaInfo(
                            templateId.toString(),
                            "test-user",
                            true,
                            false,
                            cross));
        }

        @Override
        public TopologyInfoTableResponse getTableStructure(
                String sourceName,
                String schemaName,
                String tableName) {
            structureRequests++;
            structureSourceName = sourceName;
            structureSchemaName = schemaName;
            structureTableName = tableName;
            return new TopologyInfoTableResponse(
                    new TopologyTable(
                            tableName,
                            schemaName,
                            List.of(
                                    new TopologyTableColumn(
                                            "SourceIP",
                                            "Source IP",
                                            "IPv4",
                                            true),
                                    new TopologyTableColumn(
                                            "Raw",
                                            "Raw",
                                            "String",
                                            false),
                                    new TopologyTableColumn(
                                            "Username",
                                            "Username",
                                            "String",
                                            true))));
        }

        @Override
        public QueryExecution executeTemplate(QueryType queryType, JsonNode template) {
            executeQueryType = queryType;
            filledTemplate = template;
            return new QueryExecution(RESULT_ID, TEMPLATE_ID, null, "QUEUED", null);
        }

        @Override
        public QueryResult getQueryResult(
                UUID resultId,
                QueryType queryType,
                int offset,
                int limit) {
            resultOffset = offset;
            resultLimit = limit;
            resultQueryType = queryType;
            return new QueryResult(
                    resultId,
                    queryType,
                    QueryResultState.READY,
                    200,
                    null,
                    new QueryResultMeta(
                            List.of(),
                            null,
                            "Success",
                            0,
                            null,
                            null,
                            null,
                            null,
                            List.of()),
                    List.of(),
                    offset,
                    0,
                    false);
        }
    }
}
