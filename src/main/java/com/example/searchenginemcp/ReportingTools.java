package com.example.searchenginemcp;

import com.example.searchenginemcp.dto.DatasetDescription;
import com.example.searchenginemcp.dto.DatasetSummary;
import com.example.searchenginemcp.dto.FilterValue;
import com.example.searchenginemcp.dto.QueryRequest;
import com.example.searchenginemcp.dto.QueryResult;
import com.example.searchenginemcp.dto.TopologySourcesResponse;
import com.example.searchenginemcp.service.DatasetCatalogService;
import com.example.searchenginemcp.service.ReportQueryService;
import com.example.searchenginemcp.service.SearchServiceTopologyClient;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class ReportingTools {

    private final DatasetCatalogService catalogService;
    private final ReportQueryService queryService;
    private final SearchServiceTopologyClient topologyClient;

    public ReportingTools(DatasetCatalogService catalogService,
                          ReportQueryService queryService,
                          SearchServiceTopologyClient topologyClient) {
        this.catalogService = catalogService;
        this.queryService = queryService;
        this.topologyClient = topologyClient;
    }

    @Tool(description = """
            Lists data sources available in the search service topology.
            Use this when the user asks which data storages, repositories, or sources are available.
            """)
    public TopologySourcesResponse listSearchServiceSources() {
        return topologyClient.getSources();
    }

    @Tool(description = "Lists datasets available to the current user.")
    public List<DatasetSummary> listDatasets() {
        return catalogService.listDatasets();
    }

    @Tool(description = "Describes dataset fields, filter operators, grouping options, sorting options, and metrics.")
    public DatasetDescription describeDataset(
            @ToolParam(description = "Dataset identifier, for example sales_orders.")
            String datasetId
    ) {
        return catalogService.describeDataset(datasetId);
    }

    @Tool(description = "Returns possible values for a filterable dataset field.")
    public List<FilterValue> getFilterValues(
            @ToolParam(description = "Dataset identifier, for example sales_orders.")
            String datasetId,
            @ToolParam(description = "Filterable field name, for example status.")
            String field,
            @ToolParam(description = "Optional case-insensitive search prefix.")
            String search
    ) {
        return catalogService.getFilterValues(datasetId, field, search);
    }

    @Tool(description = "Runs a read-only structured query. Raw SQL is not accepted.")
    public QueryResult runQuery(
            @Valid
            @ToolParam(description = "Structured query with datasetId, filters, groupBy, metrics, sorting, and limit.")
            QueryRequest request
    ) {
        return queryService.runQuery(request);
    }
}
