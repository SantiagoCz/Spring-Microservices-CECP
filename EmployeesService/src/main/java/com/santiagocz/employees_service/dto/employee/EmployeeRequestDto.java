package com.santiagocz.employees_service.dto.employee;

import com.santiagocz.employees_service.domain.enums.Delegation;
import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequestDto {

    @NotBlank(message = "DNI is required")
    @Pattern(regexp = "\\d{8}", message = "DNI must have 8 digits")
    private String dni;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Phone number must be valid")
    private String phone;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotNull(message = "Delegation is required")
    private Delegation delegation;

    @NotNull(message = "Role is required")
    private EmployeeRole role;
}