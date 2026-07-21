package com.example.searchenginemcp.dto.template;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QueryResult(
        UUID executionId,
        List<ResultColumn> columns,
        List<Map<String, Object>> rows,
        long totalRows,
        int offset,
        int returnedRows,
        boolean truncated) {
}
