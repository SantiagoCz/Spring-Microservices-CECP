package com.santiagocz.dental_service.controller;

import com.santiagocz.dental_service.dto.ApiResponse;
import com.santiagocz.dental_service.dto.priceList.PriceListRequestDto;
import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;
import com.santiagocz.dental_service.services.PriceListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/price-lists")
@RequiredArgsConstructor
public class PriceListController {

    private final PriceListService priceListService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<PriceListResponseDto> create(
            @Valid @RequestBody PriceListRequestDto request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(priceListService.create(request));
    }

    // ──────────── READ ────────────

    @GetMapping
    public ResponseEntity<List<PriceListResponseDto>> findAll() {
        return ResponseEntity.ok(priceListService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceListResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(priceListService.findById(id));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<PriceListResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PriceListRequestDto dto) {
        return ResponseEntity.ok(priceListService.update(id, dto));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        priceListService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "La lista de precio se ha eliminado correctamente."));
    }
}