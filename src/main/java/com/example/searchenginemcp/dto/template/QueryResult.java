package com.example.searchenginemcp.dto.template;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QueryResult(
        UUID resultId,
        QueryType queryType,
        QueryResultState state,
        int backendStatus,
        String message,
        QueryResultMeta metaInfo,
        List<Map<String, Object>> rows,
        int offset,
        int returnedRows,
        boolean truncated) {
}
