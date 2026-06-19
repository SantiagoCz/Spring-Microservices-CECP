package com.santiagocz.employees_service.dto.employee;

import com.santiagocz.employees_service.domain.enums.Delegation;
import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDto {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private Delegation delegation;
    private EmployeeRole role;
    private EmployeeStatus status;
}