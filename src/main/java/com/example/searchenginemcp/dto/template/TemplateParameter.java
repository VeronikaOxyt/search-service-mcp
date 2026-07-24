package com.example.searchenginemcp.dto.template;

import java.util.List;

public record TemplateParameter(
        String key,
        String label,
        String description,
        TemplateParameterType dataType,
        String format,
        boolean multiple,
        boolean required,
        List<String> defaultValue) {
}
