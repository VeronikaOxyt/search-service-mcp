package com.example.searchenginemcp.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mcp.security")
public class McpSecurityProperties {

    private boolean enabled = true;
    private String issuerUri;
    private String audience = "search-engine-mcp";
    private String resourceUri = "http://localhost:8081/mcp";
    private String metadataUri = "http://localhost:8081/.well-known/oauth-protected-resource";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getMetadataUri() {
        return metadataUri;
    }

    public void setMetadataUri(String metadataUri) {
        this.metadataUri = metadataUri;
    }

    public List<String> scopes() {
        return List.of(
                "sources:read",
                "templates:read",
                "templates:execute",
                "results:read");
    }
}
