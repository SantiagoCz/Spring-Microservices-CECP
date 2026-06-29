package com.santiagocz.medical_coverage_service.dto.medicalOrder;

import com.santiagocz.medical_coverage_service.domain.enums.MedicalOrderType;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedicalOrderResponseDto {
    private Long id;
    private Long number;
    private Status status;
    private MedicalOrderType medicalOrderType;
}