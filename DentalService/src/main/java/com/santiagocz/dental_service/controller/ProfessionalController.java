package com.santiagocz.dental_service.controller;

import com.santiagocz.dental_service.dto.ApiResponse;
import com.santiagocz.dental_service.dto.professional.ProfessionalRequestDto;
import com.santiagocz.dental_service.dto.professional.ProfessionalResponseDto;
import com.santiagocz.dental_service.services.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<ProfessionalResponseDto> create(
            @Valid @RequestBody ProfessionalRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(professionalService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping
    public ResponseEntity<List<ProfessionalResponseDto>> findAll() {
        return ResponseEntity.ok(professionalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(professionalService.findById(id));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalRequestDto dto) {
        return ResponseEntity.ok(professionalService.update(id, dto));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        professionalService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Profesional eliminado correctamente."));
    }
}