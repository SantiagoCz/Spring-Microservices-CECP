package com.santiagocz.medical_coverage_service.services;

import com.santiagocz.medical_coverage_service.client.AffiliateClient;
import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.entities.Payment;
import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.affiliate.AffiliateSummaryDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentDetailDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentListItemDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentRequestDto;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderResponseDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentResponseDto;
import com.santiagocz.medical_coverage_service.exceptions.EntityConflictException;
import com.santiagocz.medical_coverage_service.exceptions.EntityNotFoundException;
import com.santiagocz.medical_coverage_service.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MedicalOrderService medicalOrderService;
    private final AffiliateClient affiliateClient;

    // ──────────── CREATE ────────────

    @Transactional
    public PaymentResponseDto create(PaymentRequestDto dto) {
        validateAffiliateIsActive(dto.getAffiliateId());

        MedicalOrder medicalOrder = medicalOrderService.buildAndValidate(dto.getMedicalOrderDto());
        Payment payment = buildPayment(dto, medicalOrder);

        Payment saved = paymentRepository.save(payment);

        return buildResponseDto(saved);
    }

    // ──────────── READ (simple, no affiliate) ────────────

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findByAffiliateId(Long affiliateId) {
        return paymentRepository.findByAffiliateId(affiliateId)
                .stream()
                .map(this::buildResponseDto)
                .toList();
    }

    // ──────────── READ (listing with affiliate info) ────────────

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findByFilters(LocalDate startDate,
                                                  LocalDate endDate,
                                                  Status status,
                                                  Delegation delegation,
                                                  Long creatorId) {
        // TODO: cuando exista AuthService, aplicar lógica de rol:
        // - USER/ADMIN: forzar delegation del JWT, ignorar la del request
        // - SUPERADMIN: usar delegation del request (null = todas)

        List<Payment> payments = paymentRepository.findByFilters(
                startDate, endDate, status, delegation, creatorId);
        return toListItems(payments);
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findAllOfThisMonthForListing() {
        LocalDate[] range = getMonthRange();
        return toListItems(paymentRepository.findAllThisMonth(range[0], range[1]));
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findByDelegationThisMonthForListing(Delegation delegation) {
        LocalDate[] range = getMonthRange();
        return toListItems(paymentRepository.findByDelegationThisMonth(delegation, range[0], range[1]));
    }

    @Transactional(readOnly = true)
    public List<PaymentListItemDto> findByCreatorIdThisMonthForListing(Long creatorId) {
        LocalDate[] range = getMonthRange();
        return toListItems(paymentRepository.findByCreatorIdThisMonth(creatorId, range[0], range[1]));
    }

    // ──────────── READ (enriched detail with affiliate) ────────────

    @Transactional(readOnly = true)
    public PaymentDetailDto findById(Long id) {
        Payment payment = getEntityById(id);
        AffiliateSummaryDto affiliate = lookupSingleAffiliate(payment.getAffiliateId());
        return buildDetailDto(payment, affiliate);
    }

    @Transactional(readOnly = true)
    public PaymentDetailDto findByMedicalOrderNumber(Long orderNumber) {
        Payment payment = paymentRepository.findByMedicalOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró un pago para la orden número: " + orderNumber));
        AffiliateSummaryDto affiliate = lookupSingleAffiliate(payment.getAffiliateId());
        return buildDetailDto(payment, affiliate);
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public PaymentResponseDto update(Long paymentId, PaymentRequestDto dto) {
        Payment payment = getEntityById(paymentId);
        validateIsActive(payment);

        // TODO: validar período de edición (30 días) cuando exista AuthService con roles

        medicalOrderService.update(payment.getMedicalOrder(), dto.getMedicalOrderDto());

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

    private LocalDate[] getMonthRange() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);
        return new LocalDate[]{startOfMonth, startOfNextMonth};
    }

    private void validateAffiliateIsActive(Long affiliateId) {
        if (!Boolean.TRUE.equals(affiliateClient.isActive(affiliateId))) {
            throw new EntityConflictException("El afiliado no existe o no está activo.");
        }
    }

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

    private Payment buildPayment(PaymentRequestDto dto, MedicalOrder medicalOrder) {
        Double discountAmount = calculateDiscountAmount(dto.getAmount(), dto.getDiscount());

        return Payment.builder()
                .date(dto.getDate())
                .amount(dto.getAmount())
                .discount(dto.getDiscount())
                .discountAmount(discountAmount)
                .status(Status.ACTIVE)
                .affiliateId(dto.getAffiliateId())
                .creatorId(dto.getCreatorId()) // TODO: tomar de SecurityContext cuando exista AuthService
                .delegation(dto.getDelegation()) // TODO: tomar de SecurityContext cuando exista AuthService
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
                .creatorId(payment.getCreatorId())
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
                .creatorId(payment.getCreatorId())
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