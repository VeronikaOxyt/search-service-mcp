package com.example.searchenginemcp.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.searchenginemcp.SearchEngineMcpApplication;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = SearchEngineMcpApplication.class,
        properties = {
            "mcp.security.enabled=true",
            "mcp.security.issuer-uri=https://auth.example.test/realms/search",
            "mcp.security.audience=search-engine-mcp",
            "mcp.security.resource-uri=https://mcp.example.test/mcp",
            "mcp.security.metadata-uri=https://mcp.example.test/.well-known/oauth-protected-resource"
        })
@AutoConfigureMockMvc
class McpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    void unauthenticatedStreamableHttpRequestReturnsMcpDiscoveryChallenge() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType("application/json")
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
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "WWW-Authenticate",
                        containsString("https://mcp.example.test/.well-known/oauth-protected-resource")));
    }

    @Test
    void protectedResourceMetadataIsPublic() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("https://mcp.example.test/mcp"))
                .andExpect(jsonPath("$.authorization_servers[0]")
                        .value("https://auth.example.test/realms/search"))
                .andExpect(jsonPath("$.scopes_supported.length()").value(4));
    }

    @Test
    void methodSecurityProxyKeepsAllMcpToolsDiscoverable() {
        Set<String> names = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertEquals(5, names.size());
        assertTrue(names.contains("listSearchServiceSources"));
        assertTrue(names.contains("listSavedQueryTemplates"));
        assertTrue(names.contains("getQueryTemplateParameters"));
        assertTrue(names.contains("executeQueryTemplate"));
        assertTrue(names.contains("getQueryResult"));
    }
}
