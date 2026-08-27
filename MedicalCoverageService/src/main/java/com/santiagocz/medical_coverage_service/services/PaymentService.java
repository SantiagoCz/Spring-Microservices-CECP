package com.santiagocz.medical_coverage_service.services;

import com.santiagocz.common.context.CurrentUser;
import com.santiagocz.common.delegation.Delegation;
import com.santiagocz.common.exceptions.EntityConflictException;
import com.santiagocz.common.exceptions.EntityNotFoundException;
import com.santiagocz.medical_coverage_service.client.AffiliateClient;
import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.entities.Payment;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.affiliate.AffiliateSummaryDto;
import com.santiagocz.medical_coverage_service.dto.payment.*;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderResponseDto;
import com.santiagocz.medical_coverage_service.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final int EDIT_WINDOW_DAYS = 30;

    private final PaymentRepository paymentRepository;
    private final MedicalOrderService medicalOrderService;
    private final AffiliateClient affiliateClient;

    // ──────────── CREATE ────────────

    @Transactional
    public PaymentResponseDto create(PaymentRequestDto dto) {
        Delegation delegation = resolveDelegation(dto.getDelegation());

        validateAffiliateIsActive(dto.getAffiliateId());

        MedicalOrder medicalOrder = medicalOrderService.buildAndValidate(dto.getMedicalOrderDto(), delegation);
        Payment payment = buildPayment(dto, medicalOrder, delegation);

        return buildResponseDto(paymentRepository.save(payment));
    }

    // ──────────── READ (simple, no affiliate) ────────────

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findByAffiliateId(Long affiliateId) {
        Delegation delegation = resolveDelegationFilter(null);

        return paymentRepository.findByAffiliateId(affiliateId)
                .stream()
                .filter(p -> delegation == null || p.getDelegation() == delegation)
                .map(this::buildResponseDto)
                .toList();
    }

    // ──────────── READ (listing with affiliate info) ────────────

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findByFilters(LocalDate startDate,
                                                  LocalDate endDate,
                                                  Status status,
                                                  Delegation requestedDelegation,
                                                  Long creatorId) {
        Delegation delegation = resolveDelegationFilter(requestedDelegation);

        return toListItems(paymentRepository.findByFilters(
                startDate, endDate, status, delegation, creatorId));
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findThisMonth(Long creatorId) {
        LocalDate[] range = getMonthRange();
        return findByFilters(range[0], range[1], null, null, creatorId);
    }

    // ──────────── READ (enriched detail with affiliate) ────────────

    @Transactional(readOnly = true)
    public PaymentDetailDto findById(Long id) {
        Payment payment = getEntityById(id);
        validateSameDelegation(payment);

        return buildDetailDto(payment, lookupSingleAffiliate(payment.getAffiliateId()));
    }

    @Transactional(readOnly = true)
    public PaymentDetailDto findByMedicalOrderNumber(Long orderNumber) {
        Payment payment = paymentRepository.findByMedicalOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró un pago para la orden número: " + orderNumber));
        validateSameDelegation(payment);

        return buildDetailDto(payment, lookupSingleAffiliate(payment.getAffiliateId()));
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public PaymentResponseDto update(Long paymentId, PaymentUpdateDto dto) {
        Payment payment = getEntityById(paymentId);
        validateSameDelegation(payment);
        validateIsActive(payment);
        validateEditWindow(payment);

        medicalOrderService.update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), payment.getDelegation());

        payment.setDate(dto.getDate());
        payment.setAmount(dto.getAmount());
        payment.setDiscount(dto.getDiscount());
        payment.setDiscountAmount(calculateDiscountAmount(dto.getAmount(), dto.getDiscount()));

        return buildResponseDto(payment);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void cancel(Long paymentId) {
        Payment payment = getEntityById(paymentId);
        validateSameDelegation(payment);

        if (payment.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("El pago ya está inactivo.");
        }

        payment.setStatus(Status.INACTIVE);
        medicalOrderService.cancel(payment.getMedicalOrder());
    }

    // ──────────── PRIVATES AND AUX METHODS ────────────

    private Payment getEntityById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el pago con ID: " + id));
    }

    private void validateIsActive(Payment payment) {
        if (payment.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("No se puede modificar un pago inactivo.");
        }
    }

    private void validateEditWindow(Payment payment) {
        if (CurrentUser.hasRole(SUPER_ADMIN) || payment.getCreatedAt() == null) {
            return;
        }
        if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusDays(EDIT_WINDOW_DAYS))) {
            throw new EntityConflictException(
                    "El pago solo puede modificarse dentro de los " + EDIT_WINDOW_DAYS + " días de su registro.");
        }
    }

    private LocalDate[] getMonthRange() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return new LocalDate[]{startOfMonth, endOfMonth};
    }

    private void validateAffiliateIsActive(Long affiliateId) {
        if (!Boolean.TRUE.equals(affiliateClient.isActive(affiliateId))) {
            throw new EntityConflictException("El afiliado no existe o no está activo.");
        }
    }

    // ──────────── USER DELEGATION ────────────

    private Delegation userDelegation() {
        Delegation delegation = CurrentUser.get().delegation();

        if (delegation == null) {
            throw new EntityConflictException("El usuario autenticado no tiene una delegación asignada.");
        }
        return delegation;
    }

    private Delegation resolveDelegation(Delegation requestedDelegation) {
        if (requestedDelegation != null && CurrentUser.hasRole(SUPER_ADMIN)) {
            return requestedDelegation;
        }
        return userDelegation();
    }

    private Delegation resolveDelegationFilter(Delegation requestedDelegation) {
        if (CurrentUser.hasRole(SUPER_ADMIN)) {
            return requestedDelegation;
        }
        return userDelegation();
    }

    // Un pago de otra delegación se comporta como inexistente
    private void validateSameDelegation(Payment payment) {
        if (CurrentUser.hasRole(SUPER_ADMIN)) {
            return;
        }
        if (payment.getDelegation() != userDelegation()) {
            throw new EntityNotFoundException("No se encontró el pago con ID: " + payment.getId());
        }
    }

    // ──────────── FEIGN TO AFFILIATES ────────────

    private AffiliateSummaryDto lookupSingleAffiliate(Long affiliateId) {
        return affiliateClient.lookupByIds(List.of(affiliateId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<PaymentListItemDto> toListItems(List<Payment> payments) {
        Map<Long, AffiliateSummaryDto> affiliatesById = lookupAffiliates(payments);
        return payments.stream()
                .map(p -> buildListItemDto(p, affiliatesById.get(p.getAffiliateId())))
                .toList();
    }

    private Map<Long, AffiliateSummaryDto> lookupAffiliates(List<Payment> payments) {
        List<Long> affiliateIds = payments.stream()
                .map(Payment::getAffiliateId)
                .distinct()
                .toList();

        if (affiliateIds.isEmpty()) {
            return Map.of();
        }

        return affiliateClient.lookupByIds(affiliateIds)
                .stream()
                .collect(Collectors.toMap(AffiliateSummaryDto::getId, a -> a));
    }

    // ---- Entity Builder ----

    private Payment buildPayment(PaymentRequestDto dto, MedicalOrder medicalOrder, Delegation delegation) {
        return Payment.builder()
                .date(dto.getDate())
                .amount(dto.getAmount())
                .discount(dto.getDiscount())
                .discountAmount(calculateDiscountAmount(dto.getAmount(), dto.getDiscount()))
                .status(Status.ACTIVE)
                .affiliateId(dto.getAffiliateId())
                .delegation(delegation)
                .medicalOrder(medicalOrder)
                .build();
    }

    private Double calculateDiscountAmount(Double amount, Integer discount) {
        if (discount == null || discount == 0) {
            return 0.0;
        }
        return amount * (discount / 100.0);
    }

    // ---- Response Builders ----

    private PaymentResponseDto buildResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .date(payment.getDate())
                .amount(payment.getAmount())
                .discount(payment.getDiscount())
                .discountAmount(payment.getDiscountAmount())
                .status(payment.getStatus())
                .affiliateId(payment.getAffiliateId())
                .delegation(payment.getDelegation())
                .medicalOrder(buildMedicalOrderResponseDto(payment.getMedicalOrder()))
                .build();
    }

    private PaymentDetailDto buildDetailDto(Payment payment, AffiliateSummaryDto affiliate) {
        return PaymentDetailDto.builder()
                .id(payment.getId())
                .date(payment.getDate())
                .amount(payment.getAmount())
                .discount(payment.getDiscount())
                .discountAmount(payment.getDiscountAmount())
                .status(payment.getStatus())
                .affiliate(affiliate)
                .delegation(payment.getDelegation())
                .medicalOrder(buildMedicalOrderResponseDto(payment.getMedicalOrder()))
                .build();
    }

    private PaymentListItemDto buildListItemDto(Payment payment, AffiliateSummaryDto affiliate) {
        return PaymentListItemDto.builder()
                .id(payment.getId())
                .date(payment.getDate())
                .medicalOrderNumber(payment.getMedicalOrder().getNumber())
                .affiliateDni(affiliate != null ? affiliate.getDni() : null)
                .affiliateFullName(affiliate != null ? affiliate.getFirstName() + " " + affiliate.getLastName() : null)
                .amount(payment.getAmount())
                .discount(payment.getDiscount())
                .discountAmount(payment.getDiscountAmount())
                .status(payment.getStatus())
                .build();
    }

    private MedicalOrderResponseDto buildMedicalOrderResponseDto(MedicalOrder medicalOrder) {
        return MedicalOrderResponseDto.builder()
                .id(medicalOrder.getId())
                .number(medicalOrder.getNumber())
                .status(medicalOrder.getStatus())
                .medicalOrderType(medicalOrder.getMedicalOrderType())
                .build();
    }
}