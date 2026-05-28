package com.santiagocz.dental_service.dto.code;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CodeRequestDto(
        @NotNull Integer number,
        @NotBlank String description
) {}