package com.santiagocz.patients_service.dto;

import com.santiagocz.patients_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PatientResponseDto {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private Status status;
}