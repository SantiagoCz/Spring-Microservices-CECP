package com.santiagocz.employees_service.dto.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleBatchRequestDto {

    @NotEmpty(message = "La lista de horarios no puede estar vacía")
    @Valid
    private List<ScheduleRequestDto> schedules;

}