package com.santiagocz.appointments_service.dto.professional;

import com.santiagocz.appointments_service.domain.enums.Specialty;
import com.santiagocz.appointments_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProfessionalResponseDto {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private Specialty specialty;
    private Status status;

}