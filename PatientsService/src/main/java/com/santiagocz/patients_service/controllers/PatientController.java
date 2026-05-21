package com.santiagocz.patients_service.controllers;

import com.santiagocz.patients_service.dto.PatientResponseDto;
import com.santiagocz.patients_service.dto.PatientRequestDto;
import com.santiagocz.patients_service.services.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<PatientResponseDto> createPrimary(
            @Valid @RequestBody PatientRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patientService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @GetMapping("/by-dni/{dni}")
    public ResponseEntity<PatientResponseDto> getByDni(@PathVariable String dni) {
        return ResponseEntity.ok(patientService.getByDni(dni));
    }

    @GetMapping
    public ResponseEntity<Page<PatientResponseDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(patientService.listAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PatientResponseDto>> search(
            @RequestParam String q,
            Pageable pageable) {
        return ResponseEntity.ok(patientService.search(q, pageable));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDto dto) {
        return ResponseEntity.ok(patientService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        patientService.activate(id);
        return ResponseEntity.noContent().build();
    }
}

