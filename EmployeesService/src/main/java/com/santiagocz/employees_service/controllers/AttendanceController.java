package com.santiagocz.employees_service.controllers;

import com.santiagocz.employees_service.dto.attendance.AttendanceResponseDto;
import com.santiagocz.employees_service.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ──────────── CREATE ────────────

    @PostMapping("/checkin/{dni}")
    public ResponseEntity<AttendanceResponseDto> checkIn(@PathVariable String dni) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.checkIn(dni));
    }

    // ──────────── READ ────────────


    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceResponseDto>> findByEmployeeId(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.findByEmployeeId(employeeId));
    }

    @GetMapping("/employee/{employeeId}/between")
    public ResponseEntity<List<AttendanceResponseDto>> findByEmployeeIdBetweenDates(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.findByEmployeeIdBetweenDates(
                employeeId,
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX)));
    }

    // ──────────── UPDATE ────────────

    @PatchMapping("/checkout/{dni}")
    public ResponseEntity<AttendanceResponseDto> checkOut(
            @PathVariable String dni,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(attendanceService.checkOut(dni, notes));
    }
}