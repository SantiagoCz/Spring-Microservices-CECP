package com.santiagocz.appointments_service.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponseDto {

    private Long id;

    private Long professionalId;
    private String professionalName;

    private Long patientId;
    private String patientName;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private String status;
    private String type;

}