package com.santiagocz.dental_service.controller;

import com.santiagocz.dental_service.dto.ApiResponse;
import com.santiagocz.dental_service.dto.code.CodeRequestDto;
import com.santiagocz.dental_service.dto.code.CodeResponseDto;
import com.santiagocz.dental_service.services.CodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<CodeResponseDto> create(
            @Valid @RequestBody CodeRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(codeService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping
    public ResponseEntity<List<CodeResponseDto>> findAll() {
        return ResponseEntity.ok(codeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(codeService.findById(id));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<CodeResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CodeRequestDto dto) {
        return ResponseEntity.ok(codeService.update(id, dto));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        codeService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Codigo eliminado correctamente."));
    }
}