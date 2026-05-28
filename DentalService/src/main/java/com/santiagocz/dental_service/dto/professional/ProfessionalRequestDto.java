package com.santiagocz.dental_service.dto.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessionalRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Long priceListId
) {}
