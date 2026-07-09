package com.example.searchenginemcp;

import com.example.searchenginemcp.dto.AvailableSourcesResult;
import com.example.searchenginemcp.service.SearchServiceTopologyClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ReportingTools {

    private final SearchServiceTopologyClient topologyClient;

    public ReportingTools(SearchServiceTopologyClient topologyClient) {
        this.topologyClient = topologyClient;
    }

    @Tool(description = """
            Lists data sources available in the search service topology.
            Use this when the user asks which data storages, repositories, or sources are available.
            """)
    public AvailableSourcesResult listSearchServiceSources() {
        return topologyClient.getSources();
    }
}
