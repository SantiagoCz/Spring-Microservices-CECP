package com.santiagocz.appointments_service.controllers;

import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodRequestDto;
import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodResponseDto;
import com.santiagocz.appointments_service.services.BlockedPeriodService;
import com.santiagocz.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blocked-periods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SUB_ODONTOLOGY_CLERK')")
public class BlockedPeriodController {

    private final BlockedPeriodService blockedPeriodService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<BlockedPeriodResponseDto> create(
            @Valid @RequestBody BlockedPeriodRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(blockedPeriodService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<BlockedPeriodResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(blockedPeriodService.findById(id));
    }

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<BlockedPeriodResponseDto>> findUpcomingForProfessional(
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(blockedPeriodService.findUpcomingForProfessional(professionalId));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<BlockedPeriodResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody BlockedPeriodRequestDto dto) {
        return ResponseEntity.ok(blockedPeriodService.update(id, dto));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        blockedPeriodService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Se ha eliminado correctamente."));
    }

}