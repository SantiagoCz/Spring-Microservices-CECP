package com.santiagocz.dental_service.dto.code;

public record CodeResponseDto(
        Long id,
        Integer number,
        String description
) {}