package com.santiagocz.appointments_service.controllers;

import com.santiagocz.appointments_service.dto.ApiResponse;
import com.santiagocz.appointments_service.dto.professional.ProfessionalRequestDto;
import com.santiagocz.appointments_service.dto.professional.ProfessionalResponseDto;
import com.santiagocz.appointments_service.services.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<ProfessionalResponseDto> createPrimary(
            @Valid @RequestBody ProfessionalRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(professionalService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(professionalService.getById(id));
    }

    @GetMapping("/by-dni/{dni}")
    public ResponseEntity<ProfessionalResponseDto> getByDni(@PathVariable String dni) {
        return ResponseEntity.ok(professionalService.getByDni(dni));
    }

    @GetMapping
    public ResponseEntity<Page<ProfessionalResponseDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(professionalService.listAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProfessionalResponseDto>> search(
            @RequestParam String q,
            Pageable pageable) {
        return ResponseEntity.ok(professionalService.search(q, pageable));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalRequestDto dto) {
        return ResponseEntity.ok(professionalService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        professionalService.deactivate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Profesional dado de baja correctamente."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        professionalService.activate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Profesional dado de alta correctamente."));
    }
}