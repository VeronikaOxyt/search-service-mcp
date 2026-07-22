package com.example.searchenginemcp.dto.template;

import java.util.UUID;

public record QueryResultRequest(
        int limit,
        int offset,
        UUID resultId) {
}
