package com.example.searchenginemcp.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateMetaInfo(
        String templateId,
        String createdBy,
        Boolean isShared,
        Boolean isPersonal,
        Boolean isCross) {
}
