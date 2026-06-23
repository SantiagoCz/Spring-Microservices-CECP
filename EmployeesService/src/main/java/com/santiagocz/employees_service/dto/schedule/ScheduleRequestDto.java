package com.santiagocz.employees_service.dto.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ScheduleRequestDto {

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long employeeId;

    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;
}