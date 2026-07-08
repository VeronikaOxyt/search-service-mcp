package com.example.searchenginemcp.service;

import com.example.searchenginemcp.dto.TopologySourcesResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HttpSearchServiceTopologyClient implements SearchServiceTopologyClient {

    private final RestClient restClient;

    public HttpSearchServiceTopologyClient(RestClient searchServiceRestClient) {
        this.restClient = searchServiceRestClient;
    }

    @Override
    public TopologySourcesResponse getSources() {
        return restClient.get()
                .uri("/topology/sources")
                .retrieve()
                .body(TopologySourcesResponse.class);
    }
}
