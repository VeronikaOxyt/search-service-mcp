package com.example.searchenginemcp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record QueryRequest(
        @NotBlank String datasetId,
        List<@Valid Filter> filters,
        List<String> groupBy,
        List<@Valid Metric> metrics,
        List<@Valid Sort> sort,
        @Min(1) @Max(500) Integer limit
) {
}
