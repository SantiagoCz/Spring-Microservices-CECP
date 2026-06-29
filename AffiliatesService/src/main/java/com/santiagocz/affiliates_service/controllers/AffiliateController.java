package com.santiagocz.affiliates_service.controllers;

import com.santiagocz.affiliates_service.dto.affiliates.AffiliateRequestDto;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateResponseDto;
import com.santiagocz.affiliates_service.dto.ApiResponse;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateSummaryDto;
import com.santiagocz.affiliates_service.services.AffiliateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/affiliates")
@RequiredArgsConstructor
public class AffiliateController {

    private final AffiliateService affiliateService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<AffiliateResponseDto> createPrimary(
            @Valid @RequestBody AffiliateRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(affiliateService.createPrimary(dto));
    }

    @PostMapping("/{primaryId}/dependents")
    public ResponseEntity<AffiliateResponseDto> createDependent(
            @PathVariable Long primaryId,
            @Valid @RequestBody AffiliateRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(affiliateService.createDependent(primaryId, dto));
    }

    // ──────────── READ ────────────

    @GetMapping("/{id}")
    public ResponseEntity<AffiliateResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.getById(id));
    }

    @GetMapping("/{id}/active")
    public ResponseEntity<Boolean> isActive(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.isActive(id));
    }

    @GetMapping("/by-dni/{dni}")
    public ResponseEntity<AffiliateResponseDto> getByDni(@PathVariable String dni) {
        return ResponseEntity.ok(affiliateService.getByDni(dni));
    }

    @GetMapping
    public ResponseEntity<Page<AffiliateResponseDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(affiliateService.listAll(pageable));
    }

    @GetMapping("/primaries")
    public ResponseEntity<Page<AffiliateResponseDto>> listPrimaries(Pageable pageable) {
        return ResponseEntity.ok(affiliateService.listPrimaries(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AffiliateResponseDto>> search(
            @RequestParam String q,
            Pageable pageable) {
        return ResponseEntity.ok(affiliateService.search(q, pageable));
    }

    @GetMapping("/{id}/family")
    public ResponseEntity<List<AffiliateResponseDto>> getFamilyGroup(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.getFamilyGroup(id));
    }

    @GetMapping("/primaries/{id}/with-family")
    public ResponseEntity<AffiliateResponseDto> getPrimaryWithFamily(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.getPrimaryWithFamily(id));
    }

    @PostMapping("/lookup")
    public ResponseEntity<List<AffiliateSummaryDto>> lookupByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(affiliateService.lookupByIds(ids));
    }

    @PostMapping("/active-dnis")
    public ResponseEntity<Set<String>> filterActiveDnis(@RequestBody List<String> dnis) {
        return ResponseEntity.ok(affiliateService.filterActiveDnis(dnis));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<AffiliateResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody AffiliateRequestDto dto) {
        return ResponseEntity.ok(affiliateService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        affiliateService.deactivate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Afiliado dado de baja correctamente."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        affiliateService.activate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Afiliado dado de alta correctamente."));
    }
}
