package com.santiagocz.appointments_service.dto.schedule;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ScheduleRequestDto {

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long professionalId;

    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

    @AssertTrue(message = "La hora de inicio debe ser anterior a la hora de fin")
    private boolean isStartBeforeEnd() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return startTime.isBefore(endTime);
    }

}