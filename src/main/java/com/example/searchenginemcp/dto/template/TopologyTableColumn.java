package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopologyTableColumn(
        String columnName,
        String logicColumnName,
        String columnType,
        boolean isBaseColumn) {
}
