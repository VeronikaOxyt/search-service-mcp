package com.example.searchenginemcp.dto.template;

import java.util.List;

public record QueryResultMeta(
        List<String> baseColumns,
        String sourceName,
        String status,
        long count,
        Integer progress,
        Integer completedCount,
        Integer failedCount,
        Integer allCount,
        List<ResultColumn> columns) {
}
