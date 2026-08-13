package com.santiagocz.affiliates_service.controllers;

import com.santiagocz.common.dto.ApiResponse;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateRequestDto;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateResponseDto;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateSummaryDto;
import com.santiagocz.affiliates_service.services.AffiliateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/affiliates")
@RequiredArgsConstructor
public class AffiliateController {

    private final AffiliateService affiliateService;

    // ──────────── CREATE ────────────

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('SUB_ODONTOLOGY_CLERK', 'SUB_RRHH_ADMIN', 'SUB_MEDICAL_COVERAGE_CLERK', 'SUB_APPOINTMENTS_ADMIN')")
    @PostMapping
    public ResponseEntity<AffiliateResponseDto> createPrimary(
            @Valid @RequestBody AffiliateRequestDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(affiliateService.createPrimary(dto));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('SUB_ODONTOLOGY_CLERK', 'SUB_RRHH_ADMIN', 'SUB_MEDICAL_COVERAGE_CLERK', 'SUB_APPOINTMENTS_ADMIN')")
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

    @GetMapping("/by-dni/{dni}")
    public ResponseEntity<AffiliateResponseDto> getByDni(@PathVariable String dni) {
        return ResponseEntity.ok(affiliateService.getByDni(dni));
    }

    @GetMapping("/primaries/{id}/with-family")
    public ResponseEntity<AffiliateResponseDto> getPrimaryWithFamily(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.getPrimaryWithFamily(id));
    }

    @GetMapping("/{id}/family")
    public ResponseEntity<List<AffiliateResponseDto>> getFamilyGroup(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.getFamilyGroup(id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AffiliateResponseDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(affiliateService.listAll(pageable));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/primaries")
    public ResponseEntity<Page<AffiliateResponseDto>> listPrimaries(Pageable pageable) {
        return ResponseEntity.ok(affiliateService.listPrimaries(pageable));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<Page<AffiliateResponseDto>> search(
            @RequestParam String q,
            Pageable pageable) {
        return ResponseEntity.ok(affiliateService.search(q, pageable));
    }

    // ──────────── UPDATE ────────────

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('SUB_ODONTOLOGY_CLERK', 'SUB_RRHH_ADMIN', 'SUB_MEDICAL_COVERAGE_CLERK', 'SUB_APPOINTMENTS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AffiliateResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody AffiliateRequestDto dto) {
        return ResponseEntity.ok(affiliateService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        affiliateService.deactivate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Afiliado dado de baja correctamente."));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activate(@PathVariable Long id) {
        affiliateService.activate(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Afiliado dado de alta correctamente."));
    }

    // ──────────── FEIGN CLIENT ────────────

    @GetMapping("/{id}/active")
    public ResponseEntity<Boolean> isActive(@PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.isActive(id));
    }
    @PostMapping("/lookup")
    public ResponseEntity<List<AffiliateSummaryDto>> lookupByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(affiliateService.lookupByIds(ids));
    }

    @PostMapping("/active-dnis")
    public ResponseEntity<Set<String>> filterActiveDnis(@RequestBody List<String> dnis) {
        return ResponseEntity.ok(affiliateService.filterActiveDnis(dnis));
    }
}