package com.santiagocz.appointments_service.dto.blockePeriod;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedPeriodResponseDto {

    private Long id;
    private Long professionalId;
    private String professionalName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private Integer totalAffectedAppointments;

}