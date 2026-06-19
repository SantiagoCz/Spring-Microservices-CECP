package com.santiagocz.employees_service.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponseDto {

    private Long id;
    private Long employeeId;
    private String employeeFirstName;
    private String employeeLastName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String notes;
}