package com.example.searchenginemcp;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.example.searchenginemcp.dto.AvailableSourcesResult;
import com.example.searchenginemcp.service.SearchServiceTopologyClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {
        SearchEngineMcpApplication.class,
        McpStreamableHttpIntegrationTest.TestConfig.class
})
@AutoConfigureMockMvc
class McpStreamableHttpIntegrationTest {

    private static final String AUTHORIZATION = "Bearer test.jwt.token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingTopologyClient topologyClient;

    @Test
    void streamableHttpSessionInitializesAndListsAllTools() throws Exception {
        MvcResult initialize = mockMvc.perform(post("/mcp")
                        .contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-03-26",
                                    "capabilities": {},
                                    "clientInfo": {"name": "test", "version": "1.0"}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name").value("search-engine-mcp"))
                .andReturn();

        String sessionId = initialize.getResponse().getHeader("Mcp-Session-Id");
        assertNotNull(sessionId);

        mockMvc.perform(post("/mcp")
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-03-26")
                        .contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "method": "notifications/initialized"
                                }
                                """))
                .andExpect(status().is2xxSuccessful());

        MvcResult toolsList = mockMvc.perform(post("/mcp")
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-03-26")
                        .contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(toolsList))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(content().string(allOf(
                        containsString("listSearchServiceSources"),
                        containsString("listSavedQueryTemplates"),
                        containsString("getQueryTemplateParameters"),
                        containsString("executeQueryTemplate"),
                        containsString("getQueryResult"),
                        not(containsString("toolContext")))));

        MvcResult toolCall = mockMvc.perform(post("/mcp")
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-03-26")
                        .header("Authorization", AUTHORIZATION)
                        .contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 3,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "listSearchServiceSources",
                                    "arguments": {}
                                  }
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(toolCall))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"));

        assertEquals(AUTHORIZATION, topologyClient.authorization);
        assertEquals(1, topologyClient.invocationCount);

        MvcResult callWithoutToken = mockMvc.perform(post("/mcp")
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-03-26")
                        .contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 4,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "listSearchServiceSources",
                                    "arguments": {}
                                  }
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(callWithoutToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Authorization: Bearer <JWT> header is required")));

        assertEquals(1, topologyClient.invocationCount);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingTopologyClient recordingTopologyClient() {
            return new RecordingTopologyClient();
        }
    }

    static final class RecordingTopologyClient implements SearchServiceTopologyClient {

        private String authorization;
        private int invocationCount;

        @Override
        public AvailableSourcesResult getSources(String authorization) {
            this.authorization = authorization;
            invocationCount++;
            return new AvailableSourcesResult(List.of());
        }
    }
}
