package com.example.searchenginemcp.dto.template;

import java.util.UUID;

public record QueryExecution(
        UUID resultId,
        UUID templateId,
        QueryType queryType,
        String status,
        String message) {
}
