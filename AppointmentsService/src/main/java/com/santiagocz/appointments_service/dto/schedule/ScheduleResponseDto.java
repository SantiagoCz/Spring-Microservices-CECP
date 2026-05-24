package com.santiagocz.appointments_service.dto.schedule;

import com.santiagocz.appointments_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleResponseDto {

    private Long id;
    private Long professionalId;
    private String professionalFirstName;
    private String professionalLastName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Status status;

}