package com.example.searchenginemcp.dto.template;

import java.util.UUID;

public record TemplateSummary(
        UUID templateId,
        String name,
        String description,
        String sourceName,
        String srcTable,
        String createdBy,
        boolean isShared,
        boolean isCross) {
}
