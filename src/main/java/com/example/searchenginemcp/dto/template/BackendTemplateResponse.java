package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendTemplateResponse(
        JsonNode template,
        TemplateMetaInfo metaInfo) {
}
