package com.santiagocz.dental_service.controller;

import com.santiagocz.dental_service.dto.ApiResponse;
import com.santiagocz.dental_service.dto.commissionPrice.CommissionPriceRequestDto;
import com.santiagocz.dental_service.dto.commissionPrice.CommissionPriceResponseDto;
import com.santiagocz.dental_service.services.CommissionPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commission-prices")
@RequiredArgsConstructor
public class CommissionPriceController {

    private final CommissionPriceService commissionPriceService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<CommissionPriceResponseDto> create(
            @Valid @RequestBody CommissionPriceRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(commissionPriceService.create(dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<CommissionPriceResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(commissionPriceService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<CommissionPriceResponseDto>> findByCodeAndPriceList(
            @RequestParam Long codeId,
            @RequestParam Long priceListId) {
        return ResponseEntity.ok(commissionPriceService.findByCodeAndPriceList(codeId, priceListId));
    }

    // ──────────── DELETE ────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        commissionPriceService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Monto eliminado correctamente."));
    }
}