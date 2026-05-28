package com.santiagocz.dental_service.dto.liquidation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidationItemResponseDto(
        LocalDate date,
        String voucherNumber,
        Integer codeNumber,
        String codeDescription,
        String toothSurface,
        BigDecimal commission
) {}