package com.santiagocz.employees_service.controllers;

import com.santiagocz.employees_service.domain.enums.EmployeeRole;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import com.santiagocz.employees_service.dto.ApiResponse;
import com.santiagocz.employees_service.dto.employee.EmployeeRequestDto;
import com.santiagocz.employees_service.dto.employee.EmployeeResponseDto;
import com.santiagocz.employees_service.services.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<EmployeeResponseDto> create(@Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping
    public ResponseEntity<List<EmployeeResponseDto>> findAll() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<EmployeeResponseDto> findByDni(@PathVariable String dni) {
        return ResponseEntity.ok(employeeService.findByDni(dni));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmployeeResponseDto>> findByStatus(@PathVariable EmployeeStatus status) {
        return ResponseEntity.ok(employeeService.findByStatus(status));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<EmployeeResponseDto>> findByRole(@PathVariable EmployeeRole role) {
        return ResponseEntity.ok(employeeService.findByRole(role));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Empleado/a se ha dado de baja correctamente."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        employeeService.activate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Empleado/a se ha dado de alta correctamente."));
    }
}