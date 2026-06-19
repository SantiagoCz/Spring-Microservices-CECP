package com.santiagocz.employees_service.dto.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequestDto {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Check-in time is required")
    @PastOrPresent(message = "Check-in time cannot be in the future")
    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    private String notes;
}