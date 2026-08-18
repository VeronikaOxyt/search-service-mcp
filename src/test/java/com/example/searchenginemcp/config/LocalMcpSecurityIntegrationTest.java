package com.example.searchenginemcp.config;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.searchenginemcp.SearchEngineMcpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        classes = SearchEngineMcpApplication.class,
        properties = "mcp.security.enabled=false")
@AutoConfigureMockMvc
class LocalMcpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void localModeDoesNotExposeOAuthMetadataOrRequireAuthentication() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isNotFound());
    }

    @Test
    void streamableHttpSessionInitializesAndListsAllToolsInLocalMode() throws Exception {
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
                        containsString("getQueryResult"))));
    }
}
