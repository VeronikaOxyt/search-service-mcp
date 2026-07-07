package com.example.searchenginemcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Sort(
        @NotBlank String field,
        @NotNull SortDirection direction
) {
}
