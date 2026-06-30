package com.santiagocz.medical_coverage_service.services;

import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderRequestDto;
import com.santiagocz.medical_coverage_service.exceptions.EntityConflictException;
import com.santiagocz.medical_coverage_service.exceptions.EntityNotFoundException;
import com.santiagocz.medical_coverage_service.repositories.MedicalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalOrderService {

    private final MedicalOrderRepository medicalOrderRepository;

    // ──────────── CREATE ────────────

    MedicalOrder buildAndValidate(MedicalOrderRequestDto dto) {
        validateNumberNotInUse(dto.getNumber());

        return MedicalOrder.builder()
                .number(dto.getNumber())
                .medicalOrderType(dto.getMedicalOrderType())
                .status(Status.ACTIVE)
                .build();
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public MedicalOrder getById(Long id) {
        return medicalOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró la orden médica con ID: " + id));
    }

    @Transactional(readOnly = true)
    public MedicalOrder getActiveByNumber(Long number) {
        return medicalOrderRepository.findByNumberAndStatus(number, Status.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró una orden médica activa con número: " + number));
    }

    // ──────────── UPDATE (internal use with PaymentService) ────────────

    void update(MedicalOrder medicalOrder, MedicalOrderRequestDto dto) {
        validateNumberChangeIsAvailable(medicalOrder, dto.getNumber());

        medicalOrder.setNumber(dto.getNumber());
        medicalOrder.setMedicalOrderType(dto.getMedicalOrderType());
    }

    // ──────────── STATUS ────────────

    void cancel(MedicalOrder medicalOrder) {
        medicalOrder.setStatus(Status.INACTIVE);
    }

    // ──────────── PRIVATES AND AUX METHODS ────────────

    private void validateNumberNotInUse(Long number) {
        if (medicalOrderRepository.existsByNumberAndStatus(number, Status.ACTIVE)) {
            throw new EntityConflictException(
                    "El número de orden: " + number + " ya se encuentra registrado.");
        }
    }

    private void validateNumberChangeIsAvailable(MedicalOrder medicalOrder, Long newNumber) {
        boolean numberIsChanging = !medicalOrder.getNumber().equals(newNumber);

        if (numberIsChanging) {
            validateNumberNotInUse(newNumber);
        }
    }
}