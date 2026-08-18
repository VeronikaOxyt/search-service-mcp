package com.example.searchenginemcp.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(
        name = "mcp.security.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OAuthProtectedResourceMetadataController {

    private final McpSecurityProperties properties;

    public OAuthProtectedResourceMetadataController(McpSecurityProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/.well-known/oauth-protected-resource")
    public ProtectedResourceMetadata metadata() {
        return new ProtectedResourceMetadata(
                properties.getResourceUri(),
                List.of(properties.getIssuerUri()),
                properties.scopes(),
                List.of("header"));
    }

    public record ProtectedResourceMetadata(
            String resource,
            @JsonProperty("authorization_servers") List<String> authorizationServers,
            @JsonProperty("scopes_supported") List<String> scopesSupported,
            @JsonProperty("bearer_methods_supported") List<String> bearerMethodsSupported) {
    }
}
