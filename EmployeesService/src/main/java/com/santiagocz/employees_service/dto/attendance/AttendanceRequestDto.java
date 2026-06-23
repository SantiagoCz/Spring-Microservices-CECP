package com.santiagocz.employees_service.dto.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceRequestDto {

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long employeeId;

    @NotNull(message = "La fecha y hora de entrada es obligatoria")
    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
    private String notes;
}