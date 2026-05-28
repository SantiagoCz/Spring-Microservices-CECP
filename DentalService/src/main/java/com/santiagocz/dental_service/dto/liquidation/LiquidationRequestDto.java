package com.santiagocz.dental_service.dto.liquidation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LiquidationRequestDto(
        @NotNull Long professionalId,
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}