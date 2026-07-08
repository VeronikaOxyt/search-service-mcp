package com.example.searchenginemcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SearchEngineMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider reportingToolCallbacks(ReportingTools reportingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reportingTools)
                .build();
    }

    @Bean
    RestClient searchServiceRestClient(RestClient.Builder builder,
                                       @Value("${search-service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
