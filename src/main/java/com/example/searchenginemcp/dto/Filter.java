package com.example.searchenginemcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Filter(
        @NotBlank String field,
        @NotNull FilterOperator operator,
        @NotNull Object value
) {
}
