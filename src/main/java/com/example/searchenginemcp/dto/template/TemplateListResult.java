package com.example.searchenginemcp.dto.template;

import java.util.List;

public record TemplateListResult(long total, List<TemplateSummary> templates) {
}
