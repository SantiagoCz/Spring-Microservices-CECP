package com.santiagocz.appointments_service.dto.patient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.santiagocz.appointments_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponseDto {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private Status status;
    private Boolean affiliated;

}