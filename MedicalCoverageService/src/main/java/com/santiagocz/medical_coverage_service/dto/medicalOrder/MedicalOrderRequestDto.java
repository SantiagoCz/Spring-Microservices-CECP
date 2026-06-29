package com.santiagocz.medical_coverage_service.dto.medicalOrder;

import com.santiagocz.medical_coverage_service.domain.enums.MedicalOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MedicalOrderRequestDto {

    @NotNull(message = "El número de orden es obligatorio")
    private Long number;

    @NotNull(message = "El tipo de orden médica es obligatorio")
    private MedicalOrderType medicalOrderType;
}