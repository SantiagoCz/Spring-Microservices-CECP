package com.santiagocz.dental_service.dto.attendance;

import com.santiagocz.dental_service.dto.code.CodeResponseDto;

public record AttendanceItemResponseDto(
        Long id,
        CodeResponseDto code,
        String toothSurface
) {}