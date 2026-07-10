package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.AvailableSourcesResult;
import com.example.searchenginemcp.dto.TopologySourcesResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HttpSearchServiceTopologyClient implements SearchServiceTopologyClient {

    private final RestClient restClient;

    public HttpSearchServiceTopologyClient(RestClient searchServiceRestClient) {
        this.restClient = searchServiceRestClient;
    }

    @Override
    public AvailableSourcesResult getSources() {
        TopologySourcesResponse response = restClient.get()
                .uri("/query/topology/sources")
                .retrieve()
                .body(TopologySourcesResponse.class);

        return new AvailableSourcesResult(response == null || response.sources() == null
                ? List.of()
                : response.sources());
    }
}
