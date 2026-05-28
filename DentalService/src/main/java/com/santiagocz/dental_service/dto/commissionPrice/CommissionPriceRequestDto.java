package com.santiagocz.dental_service.dto.commissionPrice;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionPriceRequestDto(
        @NotNull Long codeId,
        @NotNull Long priceListId,
        @NotNull BigDecimal commission,
        @NotNull LocalDate validFrom,
        LocalDate validUntil
) {}