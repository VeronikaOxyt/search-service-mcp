package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendTemplateResponse(
        JsonNode template,
        TemplateMetaInfo metaInfo) {
}
