package com.example.searchenginemcp.dto;

import java.util.List;

public record AvailableSourcesResult(
        List<TopologySource> sources
) {
}
