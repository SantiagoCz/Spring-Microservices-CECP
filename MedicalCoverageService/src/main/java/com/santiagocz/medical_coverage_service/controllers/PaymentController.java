package com.santiagocz.medical_coverage_service.controllers;

import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.ApiResponse;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentDetailDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentListItemDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentRequestDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentResponseDto;
import com.santiagocz.medical_coverage_service.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ──────────── CREATE ────────────

    @PostMapping
    public ResponseEntity<PaymentResponseDto> create(@Valid @RequestBody PaymentRequestDto dto) {
        PaymentResponseDto response = paymentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ──────────── READ (simple, no affiliate) ────────────

    @GetMapping("/affiliate/{affiliateId}")
    public ResponseEntity<List<PaymentResponseDto>> findByAffiliateId(@PathVariable Long affiliateId) {
        return ResponseEntity.ok(paymentService.findByAffiliateId(affiliateId));
    }

    // ──────────── READ (listing with affiliate info) ────────────

    @GetMapping("/search")
    public ResponseEntity<List<PaymentListItemDto>> search(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Delegation delegation,
            @RequestParam(required = false) Long creatorId) {

        // TODO: cuando exista AuthService:
        // - Si rol es USER o ADMIN: ignorar delegation del request,
        //   forzar delegation del JWT
        // - Si rol es SUPERADMIN: usar delegation del request (puede ser null = todas)
        return ResponseEntity.ok(paymentService.findByFilters(
                startDate, endDate, status, delegation, creatorId));
    }

    @GetMapping
    public ResponseEntity<List<PaymentListItemDto>> findThisMonth(
            @RequestParam(required = false) Delegation delegation,
            @RequestParam(required = false) Long creatorId) {

        // TODO: reemplazar este filtro manual por lógica basada en el rol
        // del usuario autenticado (SecurityContext), cuando exista AuthService
        if (delegation != null) {
            return ResponseEntity.ok(paymentService.findByDelegationThisMonthForListing(delegation));
        }
        if (creatorId != null) {
            return ResponseEntity.ok(paymentService.findByCreatorIdThisMonthForListing(creatorId));
        }
        return ResponseEntity.ok(paymentService.findAllOfThisMonthForListing());
    }

    // ──────────── READ (enriched detail with affiliate) ────────────

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<PaymentDetailDto> findByMedicalOrderNumber(@PathVariable Long orderNumber) {
        return ResponseEntity.ok(paymentService.findByMedicalOrderNumber(orderNumber));
    }

    // ──────────── UPDATE ────────────

    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody PaymentRequestDto dto) {
        return ResponseEntity.ok(paymentService.update(id, dto));
    }

    // ──────────── STATUS ────────────

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancel(@PathVariable Long id) {
        paymentService.cancel(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Pago cancelado correctamente."));
    }
}