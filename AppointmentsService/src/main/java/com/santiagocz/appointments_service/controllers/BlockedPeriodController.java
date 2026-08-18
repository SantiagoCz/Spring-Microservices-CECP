package com.santiagocz.appointments_service.controllers;

import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodRequestDto;
import com.santiagocz.appointments_service.services.BlockedPeriodService;
import com.santiagocz.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blocked-periods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SUB_ODONTOLOGY_CLERK')")
public class BlockedPeriodController {

    private final BlockedPeriodService blockedPeriodService;

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody BlockedPeriodRequestDto dto) {
        int canceled = blockedPeriodService.blockPeriod(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse(HttpStatus.CREATED.value(),
                        "Período bloqueado. Se cancelaron " + canceled + " turnos."));
    }

    //TODO: faltan los metodos read, update, y delete

}