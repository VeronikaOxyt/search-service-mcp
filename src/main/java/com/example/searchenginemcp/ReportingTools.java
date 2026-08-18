package com.example.searchenginemcp;

import com.example.searchenginemcp.dto.AvailableSourcesResult;
import com.example.searchenginemcp.service.SearchServiceTopologyClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ReportingTools {

    private final SearchServiceTopologyClient topologyClient;

    public ReportingTools(SearchServiceTopologyClient topologyClient) {
        this.topologyClient = topologyClient;
    }

    @PreAuthorize("hasAuthority('SCOPE_sources:read')")
    @Tool(description = """
            Lists DATA SOURCES from the search-service topology: databases,
            storages and repositories that can be queried.
            Use only when the user asks about data sources, topology or storages.
            This tool does NOT return saved query templates.
            Never use it for requests containing template, saved query or шаблон;
            use listSavedQueryTemplates for those requests.
            """)
    public AvailableSourcesResult listSearchServiceSources() {
        return topologyClient.getSources();
    }
}
