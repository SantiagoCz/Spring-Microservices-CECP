package com.santiagocz.dental_service.controller;

import com.santiagocz.common.dto.ApiResponse;
import com.santiagocz.dental_service.dto.attendance.AttendanceRequestDto;
import com.santiagocz.dental_service.dto.attendance.AttendanceResponseDto;
import com.santiagocz.dental_service.services.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SUB_ODONTOLOGY_CLERK')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<AttendanceResponseDto> create(
            @Valid @RequestBody AttendanceRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(attendanceService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.findById(id));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Bono eliminado correctamente."));
    }

}