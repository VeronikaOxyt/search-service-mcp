package com.example.searchenginemcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;

public final class McpRequestAuthorization {

    public static final String TRANSPORT_CONTEXT_KEY = "authorization";

    private McpRequestAuthorization() {
    }

    public static String requireBearerToken(ToolContext toolContext) {
        String authorization = McpToolUtils.getMcpExchange(toolContext)
                .map(McpSyncServerExchange::transportContext)
                .map(context -> context.get(TRANSPORT_CONTEXT_KEY))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authorization: Bearer <JWT> header is required for MCP tool calls"));

        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())
                || authorization.substring(prefix.length()).isBlank()) {
            throw new IllegalArgumentException(
                    "Authorization header must use the Bearer scheme");
        }
        if (authorization.indexOf('\r') >= 0 || authorization.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Authorization header contains invalid characters");
        }

        return "Bearer " + authorization.substring(prefix.length()).trim();
    }
}
