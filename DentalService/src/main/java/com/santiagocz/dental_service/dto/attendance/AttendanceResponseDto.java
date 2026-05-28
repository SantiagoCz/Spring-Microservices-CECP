package com.santiagocz.dental_service.dto.attendance;

import com.santiagocz.dental_service.dto.professional.ProfessionalResponseDto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceResponseDto(
        Long id,
        LocalDate date,
        String voucherNumber,
        ProfessionalResponseDto professional,
        Long appointmentId,
        List<AttendanceItemResponseDto> items
) {}