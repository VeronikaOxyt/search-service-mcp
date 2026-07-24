package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopologyInfoTableResponse(TopologyTable tableInfo) {
}
