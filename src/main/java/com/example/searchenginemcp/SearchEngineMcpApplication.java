package com.example.searchenginemcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
}
