package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultColumn(
        String name,
        String type,
        @JsonAlias({"LogicColumnName", "logicColumnName"}) String logicColumnName,
        String attributeCode) {
}
