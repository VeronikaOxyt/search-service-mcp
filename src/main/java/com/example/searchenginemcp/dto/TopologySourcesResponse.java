package com.example.searchenginemcp.dto;

import java.util.List;

public record TopologySourcesResponse(
        Integer status,
        Object timestamp,
        List<TopologySource> sources
) {
}
