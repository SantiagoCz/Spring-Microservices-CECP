package com.santiagocz.dental_service.dto.attendance;

import jakarta.validation.constraints.NotNull;

public record AttendanceItemRequestDto(
        @NotNull Long codeId,
        String toothSurface
) {}
