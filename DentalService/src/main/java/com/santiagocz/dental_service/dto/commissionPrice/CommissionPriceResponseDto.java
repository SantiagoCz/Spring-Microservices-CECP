package com.santiagocz.dental_service.dto.commissionPrice;

import com.santiagocz.dental_service.dto.code.CodeResponseDto;
import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionPriceResponseDto(
        Long id,
        CodeResponseDto code,
        PriceListResponseDto priceList,
        BigDecimal commission,
        LocalDate validFrom,
        LocalDate validUntil
) {}