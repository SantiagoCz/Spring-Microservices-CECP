package com.santiagocz.medical_coverage_service.services;

import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderRequestDto;
import com.santiagocz.medical_coverage_service.exceptions.EntityConflictException;
import com.santiagocz.medical_coverage_service.repositories.MedicalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalOrderService {

    private final MedicalOrderRepository medicalOrderRepository;

    // ──────────── CREATE ────────────

    MedicalOrder buildAndValidate(MedicalOrderRequestDto dto, Delegation delegation) {
        validateNumberNotInUse(dto.getNumber(), delegation);
        return MedicalOrder.builder()
                .number(dto.getNumber())
                .medicalOrderType(dto.getMedicalOrderType())
                .status(Status.ACTIVE)
                .build();
    }

    // ──────────── UPDATE ────────────

    void update(MedicalOrder medicalOrder, MedicalOrderRequestDto dto, Delegation delegation) {
        validateNumberChangeIsAvailable(medicalOrder, dto.getNumber(), delegation);
        medicalOrder.setNumber(dto.getNumber());
        medicalOrder.setMedicalOrderType(dto.getMedicalOrderType());
    }

    // ──────────── STATUS ────────────

    void cancel(MedicalOrder medicalOrder) {
        medicalOrder.setStatus(Status.INACTIVE);
    }

    // ──────────── PRIVATES AND AUX METHODS ────────────

    private void validateNumberNotInUse(Long number, Delegation delegation) {
        if (medicalOrderRepository.existsByNumberAndStatusAndDelegation(
                number, Status.ACTIVE, delegation)) {
            throw new EntityConflictException(
                    "El número de orden: " + number + " ya se encuentra registrado.");
        }
    }

    private void validateNumberChangeIsAvailable(MedicalOrder medicalOrder,
                                                 Long newNumber,
                                                 Delegation delegation) {
        boolean numberIsChanging = !medicalOrder.getNumber().equals(newNumber);
        if (numberIsChanging) {
            validateNumberNotInUse(newNumber, delegation);
        }
    }
}