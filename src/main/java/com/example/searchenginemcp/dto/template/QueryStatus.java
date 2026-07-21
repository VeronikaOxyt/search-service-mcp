package com.example.searchenginemcp.dto.template;

import java.util.UUID;

public record QueryStatus(
        UUID executionId,
        String status,
        String message,
        Long rowCount) {
}
