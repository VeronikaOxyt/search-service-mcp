package com.example.searchenginemcp.dto.template;

import java.util.List;
import java.util.UUID;

public record TemplateExecutionSchema(
        UUID templateId,
        long version,
        String name,
        String description,
        QueryType queryType,
        List<TemplateParameter> parameters) {
}
