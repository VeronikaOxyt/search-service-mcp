package com.example.searchenginemcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryResultState;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.BackendTemplateResponse;
import com.example.searchenginemcp.dto.template.TopologyInfoTableResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpTemplateBackendClientTest {

    private static final UUID RESULT_ID = UUID.fromString("b2d50775-ae11-4d91-b98c-f0a4cb2f6059");
    private static final UUID TEMPLATE_ID = UUID.fromString("92de4773-7a00-4000-8000-000000000000");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockRestServiceServer server;
    private HttpTemplateBackendClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://backend.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpTemplateBackendClient(builder.build());
    }

    @Test
    void loadsRawTemplateByIdWithoutDroppingItsTree() {
        server.expect(requestTo(
                        "http://backend.test/mid/template?id=92de4773-7a00-4000-8000-000000000000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "template": {
                                    "name": "User events",
                                    "where": {
                                      "filters": [
                                        {
                                          "column": "Username",
                                          "operator": "Equals",
                                          "value": [""],
                                          "backendSpecificField": "preserved"
                                        }
                                      ]
                                    }
                                  },
                                  "metaInfo": {
                                    "templateId": "92de4773-7a00-4000-8000-000000000000",
                                    "isCross": false
                                  }
                                }
                                """));

        BackendTemplateResponse response = client.getTemplate(TEMPLATE_ID);

        assertEquals("User events", response.template().path("name").asText());
        assertEquals(
                "preserved",
                response.template()
                        .at("/where/filters/0/backendSpecificField")
                        .asText());
        assertFalse(response.metaInfo().isCross());
        server.verify();
    }

    @Test
    void loadsTableStructureForRegularQuery() {
        server.expect(requestTo(
                        "http://backend.test/mid/query/topology/structureTable"
                                + "?schemaName=log_armatm_src_distr"
                                + "&tableName=parsed"
                                + "&sourceName=datastore_clickhouse"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "tableInfo": {
                                    "tableName": "parsed",
                                    "schemaName": "log_armatm_src_distr",
                                    "columns": [
                                      {
                                        "columnName": "SourceIP",
                                        "logicColumnName": "Source IP",
                                        "columnType": "IPv4",
                                        "isBaseColumn": true
                                      },
                                      {
                                        "columnName": "raw",
                                        "columnType": "String",
                                        "isBaseColumn": false
                                      }
                                    ]
                                  }
                                }
                                """));

        TopologyInfoTableResponse response = client.getTableStructure(
                "datastore_clickhouse",
                "log_armatm_src_distr",
                "parsed");

        assertTrue(response.tableInfo().columns().getFirst().isBaseColumn());
        assertFalse(response.tableInfo().columns().get(1).isBaseColumn());
        server.verify();
    }

    @Test
    void submitsRegularQueryToItsExecutionEndpoint() throws Exception {
        server.expect(requestTo(
                        "http://backend.test/mid/query/executeQuery"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "name": "User events",
                          "where": {
                            "filters": [
                              {
                                "column": "Username",
                                "value": ["ivanov"]
                              }
                            ]
                          }
                        }
                        """))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "resultId": "b2d50775-ae11-4d91-b98c-f0a4cb2f6059",
                                  "templateId": "92de4773-7a00-4000-8000-000000000000",
                                  "status": "QUEUED"
                                }
                                """));

        client.executeTemplate(
                QueryType.QUERY,
                OBJECT_MAPPER.readTree("""
                        {
                          "name": "User events",
                          "where": {
                            "filters": [
                              {
                                "column": "Username",
                                "value": ["ivanov"]
                              }
                            ]
                          }
                        }
                        """));

        server.verify();
    }

    @Test
    void submitsCrossQueryToItsExecutionEndpoint() throws Exception {
        server.expect(requestTo("http://backend.test/mid/query/executeCrossQuery"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "sourceName": "datastore_clickhouse",
                          "schemaName": ["schema_a", "schema_b"]
                        }
                        """))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "resultId": "b2d50775-ae11-4d91-b98c-f0a4cb2f6059",
                                  "templateId": "92de4773-7a00-4000-8000-000000000000",
                                  "status": "QUEUED"
                                }
                                """));

        client.executeTemplate(
                QueryType.CROSS,
                OBJECT_MAPPER.readTree("""
                        {
                          "sourceName": "datastore_clickhouse",
                          "schemaName": ["schema_a", "schema_b"]
                        }
                        """));

        server.verify();
    }

    @Test
    void mapsHttp425ForRegularQueryToPending() {
        server.expect(requestTo("http://backend.test/mid/query/result"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"limit":50,"offset":0,"resultId":"b2d50775-ae11-4d91-b98c-f0a4cb2f6059"}
                        """))
                .andRespond(withStatus(HttpStatus.TOO_EARLY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "status": 425,
                                  "message": "Запрос в процессе выполнения",
                                  "result": {
                                    "metaInfo": {
                                      "baseColumns": ["SourceIP", "Username"],
                                      "sourceName": "datastore_clickhouse",
                                      "status": "Processing",
                                      "count": 0
                                    }
                                  }
                                }
                                """));

        QueryResult result = client.getQueryResult(RESULT_ID, QueryType.QUERY, 0, 50);

        assertEquals(QueryResultState.PENDING, result.state());
        assertEquals(425, result.backendStatus());
        assertEquals("Processing", result.metaInfo().status());
        assertTrue(result.rows().isEmpty());
        server.verify();
    }

    @Test
    void mapsCompletedCrossQueryAndPaginationMetadata() {
        server.expect(requestTo("http://backend.test/mid/query/crossResult"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"limit":2,"offset":0,"resultId":"b2d50775-ae11-4d91-b98c-f0a4cb2f6059"}
                        """))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "status": 200,
                                  "crossResult": {
                                    "metaInfo": {
                                      "sourceName": "datastore_clickhouse",
                                      "status": "Completed",
                                      "count": 3,
                                      "progress": 100,
                                      "completedCount": 3,
                                      "failedCount": 0,
                                      "allCount": 3,
                                      "columns": [
                                        {"name": "SourceIP", "type": "IPv4"}
                                      ]
                                    },
                                    "resultData": [
                                      {"SourceIP": "10.0.0.1"},
                                      {"SourceIP": "10.0.0.2"}
                                    ]
                                  }
                                }
                                """));

        QueryResult result = client.getQueryResult(RESULT_ID, QueryType.CROSS, 0, 2);

        assertEquals(QueryResultState.READY, result.state());
        assertEquals(2, result.returnedRows());
        assertEquals(100, result.metaInfo().progress());
        assertEquals("IPv4", result.metaInfo().columns().getFirst().type());
        assertTrue(result.truncated());
        assertFalse(result.rows().isEmpty());
        server.verify();
    }
}
