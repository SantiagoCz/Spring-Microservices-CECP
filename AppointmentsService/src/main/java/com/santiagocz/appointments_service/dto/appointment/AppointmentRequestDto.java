package com.santiagocz.appointments_service.dto.appointment;

import com.santiagocz.appointments_service.domain.enums.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequestDto {

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long professionalId;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    @Future(message = "El turno debe ser en el futuro")
    private LocalDateTime startDateTime;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser positiva")
    private Integer durationMinutes;

    @NotNull(message = "El tipo de turno es obligatorio")
    private AppointmentType type;

}