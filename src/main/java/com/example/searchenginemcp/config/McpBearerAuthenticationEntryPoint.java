package com.example.searchenginemcp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class McpBearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final McpSecurityProperties properties;

    public McpBearerAuthenticationEntryPoint(McpSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        response.setHeader(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"%s\""
                        .formatted(properties.getMetadataUri()));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
