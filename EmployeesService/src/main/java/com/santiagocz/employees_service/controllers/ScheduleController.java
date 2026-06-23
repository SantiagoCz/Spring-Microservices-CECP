package com.santiagocz.employees_service.controllers;

import com.santiagocz.employees_service.dto.ApiResponse;
import com.santiagocz.employees_service.dto.schedule.ScheduleBatchRequestDto;
import com.santiagocz.employees_service.dto.schedule.ScheduleRequestDto;
import com.santiagocz.employees_service.dto.schedule.ScheduleResponseDto;
import com.santiagocz.employees_service.services.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<ScheduleResponseDto> create(
            @Valid @RequestBody ScheduleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(dto));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ScheduleResponseDto>> createBatch(
            @Valid @RequestBody ScheduleBatchRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createBatch(request.getSchedules()));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.findById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ScheduleResponseDto>> findByEmployeeId(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(scheduleService.findByEmployeeId(employeeId));
    }

    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List<ScheduleResponseDto>> findByDayOfWeek(
            @PathVariable DayOfWeek dayOfWeek) {
        return ResponseEntity.ok(scheduleService.findByDayOfWeek(dayOfWeek));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRequestDto dto) {
        return ResponseEntity.ok(scheduleService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Horario eliminado correctamente."));
    }

}