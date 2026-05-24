package com.santiagocz.appointments_service.dto.appointment;

import com.santiagocz.appointments_service.domain.enums.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequestDto {

    @NotNull(message = "Professional ID is required")
    private Long professionalId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Start date/time is required")
    @Future(message = "El turno debe ser en el futuro")
    private LocalDateTime startDateTime;

    @NotNull(message = "Duration is required")
    @Positive(message = "La duración debe ser positiva")
    private Integer durationMinutes;

    @NotNull(message = "Appointment type is required")
    private AppointmentType type;

}