package com.example.searchenginemcp.dto;

import java.util.List;

public record MetricDescription(
        String field,
        String label,
        List<Aggregation> aggregations
) {
}
