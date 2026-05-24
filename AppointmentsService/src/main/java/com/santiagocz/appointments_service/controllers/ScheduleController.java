package com.santiagocz.appointments_service.controllers;

import com.santiagocz.appointments_service.dto.ApiResponse;
import com.santiagocz.appointments_service.dto.schedule.ScheduleBatchRequestDto;
import com.santiagocz.appointments_service.dto.schedule.ScheduleRequestDto;
import com.santiagocz.appointments_service.dto.schedule.ScheduleResponseDto;
import com.santiagocz.appointments_service.services.ScheduleService;
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

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<ScheduleResponseDto>> findByProfessionalId(
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(scheduleService.findByProfessionalId(professionalId));
    }

    @GetMapping("/actives-by-professional/{professionalId}")
    public ResponseEntity<List<ScheduleResponseDto>> findActivesByProfessionalId(
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(scheduleService.findActivesByProfessionalId(professionalId));
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

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        scheduleService.deactivate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Horario dado de baja correctamente."));
    }

}