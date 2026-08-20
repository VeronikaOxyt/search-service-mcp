package com.example.searchenginemcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import java.util.Map;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@SpringBootApplication
public class SearchEngineMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider toolCallbacks(
            ReportingTools reportingTools,
            TemplateTools templateTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reportingTools, templateTools)
                .build();
    }

    @Bean
    RestClient searchServiceRestClient(RestClient.Builder builder,
                                       @Value("${search-service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties properties) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .mcpEndpoint(properties.getMcpEndpoint())
                .keepAliveInterval(properties.getKeepAliveInterval())
                .disallowDelete(properties.isDisallowDelete())
                .contextExtractor(request -> {
                    String authorization = request.headers()
                            .firstHeader(HttpHeaders.AUTHORIZATION);
                    return authorization == null || authorization.isBlank()
                            ? McpTransportContext.EMPTY
                            : McpTransportContext.create(Map.of(
                                    McpRequestAuthorization.TRANSPORT_CONTEXT_KEY,
                                    authorization));
                })
                .build();
    }
}
