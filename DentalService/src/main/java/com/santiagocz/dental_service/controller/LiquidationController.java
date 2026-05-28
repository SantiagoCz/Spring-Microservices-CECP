package com.santiagocz.dental_service.controller;

import com.santiagocz.dental_service.dto.liquidation.LiquidationRequestDto;
import com.santiagocz.dental_service.dto.liquidation.LiquidationResponseDto;
import com.santiagocz.dental_service.services.LiquidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/liquidations")
@RequiredArgsConstructor
public class LiquidationController {

    private final LiquidationService liquidationService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<LiquidationResponseDto> calculate(
            @Valid @RequestBody LiquidationRequestDto dto) {
        return ResponseEntity
                .ok(liquidationService.calculate(dto));
    }
}