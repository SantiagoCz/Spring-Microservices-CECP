package com.santiagocz.appointments_service.controllers;

import com.santiagocz.appointments_service.dto.ApiResponse;
import com.santiagocz.appointments_service.dto.appointment.AppointmentRequestDto;
import com.santiagocz.appointments_service.dto.appointment.AppointmentResponseDto;
import com.santiagocz.appointments_service.services.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> create(
            @Valid @RequestBody AppointmentRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointmentService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<AppointmentResponseDto>> findByProfessionalId(
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(appointmentService.findByProfessionalId(professionalId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponseDto>> findByPatientId(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.findByPatientId(patientId));
    }

    @GetMapping("/professional/{professionalId}/agenda")
    public ResponseEntity<List<AppointmentResponseDto>> findProfessionalAgenda(
            @PathVariable Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.findProfessionalAgenda(professionalId, date));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequestDto dto) {
        return ResponseEntity.ok(appointmentService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse> confirm(@PathVariable Long id) {
        appointmentService.confirm(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Turno confirmado correctamente."));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Turno cancelado correctamente."));
    }

    @PatchMapping("/{id}/attended")
    public ResponseEntity<ApiResponse> markAttended(@PathVariable Long id) {
        appointmentService.markAttended(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Turno marcado como atendido."));
    }
}