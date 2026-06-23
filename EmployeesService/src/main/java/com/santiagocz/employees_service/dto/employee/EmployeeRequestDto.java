package com.santiagocz.employees_service.dto.employee;

import com.santiagocz.employees_service.domain.enums.Delegation;
import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeRequestDto {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "El teléfono no es válido")
    private String phone;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate birthDate;

    @NotNull(message = "La delegación es obligatoria")
    private Delegation delegation;

    @NotNull(message = "El rol es obligatorio")
    private EmployeeRole role;
}