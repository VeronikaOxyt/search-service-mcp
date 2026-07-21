package com.example.searchenginemcp.dto.template;

import java.util.Map;

public record ExecuteTemplateRequest(
        long templateVersion,
        Map<String, Object> parameters) {
}
