package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.Code;
import com.santiagocz.dental_service.domain.entities.CommissionPrice;
import com.santiagocz.dental_service.domain.entities.PriceList;
import com.santiagocz.dental_service.dto.code.CodeResponseDto;
import com.santiagocz.dental_service.dto.commissionPrice.CommissionPriceRequestDto;
import com.santiagocz.dental_service.dto.commissionPrice.CommissionPriceResponseDto;
import com.santiagocz.dental_service.dto.priceList.PriceListResponseDto;
import com.santiagocz.dental_service.exceptions.EntityConflictException;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.CommissionPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommissionPriceService {

    private final CommissionPriceRepository commissionPriceRepository;
    private final CodeService codeService;
    private final PriceListService priceListService;

    // ──────────── CREATE ────────────

    @Transactional
    public CommissionPriceResponseDto create(CommissionPriceRequestDto dto) {
        Code code = codeService.getEntityById(dto.codeId());
        PriceList priceList = priceListService.getEntityById(dto.priceListId());

        if (dto.validUntil() != null && !dto.validUntil().isAfter(dto.validFrom())) {
            throw new EntityConflictException(
                    "La fecha de fin de vigencia debe ser posterior a la fecha de inicio.");
        }

        // Si existe una comisión activa para el mismo código y lista, la cierra
        commissionPriceRepository
                .findActiveByCodeAndPriceListAndDate(code.getId(), priceList.getId(), dto.validFrom())
                .ifPresent(active -> {
                    active.setValidUntil(dto.validFrom().minusDays(1));
                    commissionPriceRepository.save(active);
                });

        CommissionPrice commissionPrice = new CommissionPrice();
        commissionPrice.setCode(code);
        commissionPrice.setPriceList(priceList);
        commissionPrice.setCommission(dto.commission());
        commissionPrice.setValidFrom(dto.validFrom());
        commissionPrice.setValidUntil(dto.validUntil());
        return toResponse(commissionPriceRepository.save(commissionPrice));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public CommissionPriceResponseDto findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CommissionPriceResponseDto> findByCodeAndPriceList(Long codeId, Long priceListId) {
        codeService.getEntityById(codeId);
        priceListService.getEntityById(priceListId);
        return commissionPriceRepository
                .findByCodeIdAndPriceListId(codeId, priceListId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ──────────── DELETE ────────────

    public void delete(Long id) {
        getEntityById(id);
        commissionPriceRepository.deleteById(id);
    }

    // ──────────── PRIVATES ────────────

    private CommissionPrice getEntityById(Long id) {
        return commissionPriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Monto no encontrado con id: " + id));
    }

    private CommissionPriceResponseDto toResponse(CommissionPrice commissionPrice) {
        return new CommissionPriceResponseDto(
                commissionPrice.getId(),
                new CodeResponseDto(commissionPrice.getCode().getId(), commissionPrice.getCode().getNumber(), commissionPrice.getCode().getDescription()),
                new PriceListResponseDto(commissionPrice.getPriceList().getId(), commissionPrice.getPriceList().getName()),
                commissionPrice.getCommission(),
                commissionPrice.getValidFrom(),
                commissionPrice.getValidUntil()
        );
    }
}