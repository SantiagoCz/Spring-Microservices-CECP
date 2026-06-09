package com.santiagocz.affiliates_service.services;

import com.santiagocz.affiliates_service.component.PayslipMapper;
import com.santiagocz.affiliates_service.dto.paylips.PayslipRequestDto;
import com.santiagocz.affiliates_service.dto.paylips.PayslipResponseDto;
import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.entities.Payslip;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.repositories.AffiliateRepository;
import com.santiagocz.affiliates_service.repositories.PayslipRepository;
import com.santiagocz.affiliates_service.exceptions.AffiliateConflictException;
import com.santiagocz.affiliates_service.exceptions.AffiliateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final AffiliateRepository affiliateRepository;
    private final PayslipStorageService storageService;
    private final PayslipMapper mapper;

    // ──────────── UPLOAD ────────────

    @Transactional
    public PayslipResponseDto upload(PayslipRequestDto dto, MultipartFile file) {
        Affiliate primary = resolvePrimary(dto.getAffiliateId());

        if (payslipRepository.existsByPrimaryAffiliate_IdAndPeriod(primary.getId(), dto.getPeriod())) {
            throw new AffiliateConflictException(
                    "Ya existe un recibo para el período " + dto.getPeriod() + " del titular");
        }

        Payslip payslip = Payslip.builder()
                .period(dto.getPeriod())
                .primaryAffiliate(primary)
                .uploadDate(LocalDateTime.now())
                .build();
        payslip = payslipRepository.save(payslip);

        String fileName = storageService.save(file, payslip.getId());
        payslip.setFileName(fileName);

        return mapper.toResponse(payslipRepository.save(payslip));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public PayslipResponseDto getById(Long id) {
        return mapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public PayslipResponseDto getLatestByAffiliate(Long affiliateId) {
        Affiliate primary = resolvePrimary(affiliateId);
        return payslipRepository
                .findTopByPrimaryAffiliate_IdOrderByPeriodDesc(primary.getId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "No hay recibos cargados para el titular ID " + primary.getId()));
    }

    @Transactional(readOnly = true)
    public List<PayslipResponseDto> getAllByAffiliate(Long affiliateId) {
        Affiliate primary = resolvePrimary(affiliateId);
        return payslipRepository
                .findAllByPrimaryAffiliate_IdOrderByPeriodDesc(primary.getId())
                .stream().map(mapper::toResponse).toList();
    }

    public byte[] downloadFile(Long payslipId) {
        Payslip payslip = getEntityById(payslipId);
        return storageService.read(payslip.getFileName());
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        Payslip payslip = getEntityById(id);
        storageService.delete(payslip.getFileName());
        payslipRepository.delete(payslip);
    }

    // ──────────── INTERNOS ────────────

    private Payslip getEntityById(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "Recibo no encontrado con ID: " + id));
    }

    private Affiliate resolvePrimary(Long affiliateId) {
        Affiliate affiliate = affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new AffiliateNotFoundException(
                        "No se encontró el afiliado con ID: " + affiliateId));

        return (affiliate.getAffiliateType() == AffiliateType.PRIMARY)
                ? affiliate
                : affiliate.getPrimaryAffiliate();
    }

}