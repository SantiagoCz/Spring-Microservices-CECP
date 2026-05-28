package com.santiagocz.dental_service.dto.professional;

import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;

public record ProfessionalResponseDto(
        Long id,
        String name,
        PriceListResponseDto priceList
) {}