package com.santiagocz.appointments_service.dto.professional;

import com.santiagocz.appointments_service.domain.enums.Specialty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfessionalRequestDto {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "La matrícula es obligatoria")
    @Pattern(regexp = "\\d+", message = "La matrícula debe ser numérica")
    private String licenseNumber;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres")
    private String lastName;

    @Pattern(regexp = "^(\\d{1,11})?$", message = "El teléfono debe tener entre 1 y 11 dígitos")
    private String phoneNumber;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser pasada")
    private LocalDate birthDate;

    @NotNull(message = "La especialidad es obligatoria")
    private Specialty specialty;

}