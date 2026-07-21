package com.example.searchenginemcp.dto.template;

import java.util.UUID;

public record QueryExecution(
        UUID executionId,
        UUID templateId,
        String status,
        String message) {
}
