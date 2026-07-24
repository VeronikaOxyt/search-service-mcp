package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopologyTable(
        String tableName,
        String schemaName,
        List<TopologyTableColumn> columns) {
}
