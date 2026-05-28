package com.santiagocz.dental_service.dto.priceList;

import jakarta.validation.constraints.NotBlank;

public record PriceListRequestDto(
        @NotBlank String name
) {}