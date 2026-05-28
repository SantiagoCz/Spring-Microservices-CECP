package com.santiagocz.dental_service.services;

import com.santiagocz.dental_service.domain.entities.Attendance;
import com.santiagocz.dental_service.domain.entities.AttendanceItem;
import com.santiagocz.dental_service.domain.entities.CommissionPrice;
import com.santiagocz.dental_service.domain.entities.Professional;
import com.santiagocz.dental_service.dto.liquidation.LiquidationItemResponseDto;
import com.santiagocz.dental_service.dto.liquidation.LiquidationRequestDto;
import com.santiagocz.dental_service.dto.liquidation.LiquidationResponseDto;
import com.santiagocz.dental_service.exceptions.EntityConflictException;
import com.santiagocz.dental_service.exceptions.EntityNotFoundException;
import com.santiagocz.dental_service.repositories.AttendanceRepository;
import com.santiagocz.dental_service.repositories.CommissionPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiquidationService {

    private final AttendanceRepository attendanceRepository;
    private final CommissionPriceRepository commissionPriceRepository;
    private final ProfessionalService professionalService;

    // ──────────── CREATE ────────────

    @Transactional
    public LiquidationResponseDto calculate(LiquidationRequestDto dto) {
        if (!dto.to().isAfter(dto.from())) {
            throw new EntityConflictException(
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }

        Professional professional = professionalService.getEntityById(dto.professionalId());
        Long priceListId = professional.getPriceList().getId();

        List<Attendance> attendances = attendanceRepository
                .findByProfessionalIdAndDateBetween(
                        professional.getId(), dto.from(), dto.to());

        List<LiquidationItemResponseDto> items = new ArrayList<>();

        for (Attendance attendance : attendances) {
            for (AttendanceItem item : attendance.getItems()) {
                CommissionPrice commissionPrice = commissionPriceRepository
                        .findActiveByCodeAndPriceListAndDate(
                                item.getCode().getId(), priceListId, attendance.getDate())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "No se encontró comisión vigente para el código "
                                        + item.getCode().getNumber()
                                        + " en la lista '"
                                        + professional.getPriceList().getName()
                                        + "' para la fecha " + attendance.getDate()));

                items.add(new LiquidationItemResponseDto(
                        attendance.getDate(),
                        attendance.getVoucherNumber(),
                        item.getCode().getNumber(),
                        item.getCode().getDescription(),
                        item.getToothSurface(),
                        commissionPrice.getCommission()
                ));
            }
        }

        BigDecimal total = items.stream()
                .map(LiquidationItemResponseDto::commission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LiquidationResponseDto(
                professionalService.toResponse(professional),
                dto.from(),
                dto.to(),
                items,
                total
        );
    }
}