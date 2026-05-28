package com.santiagocz.dental_service.dto.liquidation;

import com.santiagocz.dental_service.dto.professional.ProfessionalResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LiquidationResponseDto(
        ProfessionalResponseDto professional,
        LocalDate from,
        LocalDate to,
        List<LiquidationItemResponseDto> items,
        BigDecimal total
) {}