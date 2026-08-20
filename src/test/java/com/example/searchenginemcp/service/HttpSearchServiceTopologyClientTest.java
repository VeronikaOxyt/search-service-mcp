package com.example.searchenginemcp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpSearchServiceTopologyClientTest {

    private static final String AUTHORIZATION = "Bearer test.jwt.token";

    private MockRestServiceServer server;
    private HttpSearchServiceTopologyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://backend.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpSearchServiceTopologyClient(builder.build());
    }

    @Test
    void forwardsAuthorizationWhenLoadingSources() {
        server.expect(requestTo("http://backend.test/query/topology/sources"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTHORIZATION))
                .andRespond(withSuccess("{\"sources\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(client.getSources(AUTHORIZATION).sources().isEmpty());
        server.verify();
    }
}
