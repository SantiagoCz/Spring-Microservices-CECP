package com.santiagocz.appointments_service.dto.appointment;

import com.santiagocz.appointments_service.domain.enums.AppointmentType;
import jakarta.validation.constraints.*;
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
    @Min(value = 30, message = "La duración mínima es 30 minutos")
    @Max(value = 60, message = "La duración máxima es 60 minutos")
    private Integer durationMinutes;

    @NotNull(message = "El tipo de turno es obligatorio")
    private AppointmentType type;

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String notes;

}