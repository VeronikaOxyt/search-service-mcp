package com.example.searchenginemcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Metric(
        @NotBlank String field,
        @NotNull Aggregation aggregation
) {
}
