package com.example.searchenginemcp.dto;

import java.util.List;

public record DatasetDescription(
        String id,
        String name,
        String description,
        List<FieldDescription> fields,
        List<String> groupableFields,
        List<String> sortableFields,
        List<MetricDescription> metrics
) {
}
