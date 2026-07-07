package com.example.searchenginemcp.dto;

import java.util.List;

public record FieldDescription(
        String name,
        FieldType type,
        String description,
        boolean filterable,
        boolean groupable,
        boolean sortable,
        List<FilterOperator> operators
) {
}
