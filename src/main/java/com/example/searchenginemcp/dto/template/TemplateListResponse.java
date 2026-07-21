package com.example.searchenginemcp.dto.template;

import java.util.List;

public record TemplateListResponse(
        int status,
        Object timestamp,
        long templateCount,
        List<TemplateSummary> templateList) {
}
