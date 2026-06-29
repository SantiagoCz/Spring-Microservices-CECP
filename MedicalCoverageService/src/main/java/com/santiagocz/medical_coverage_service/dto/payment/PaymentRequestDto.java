package com.santiagocz.medical_coverage_service.dto.payment;

import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentRequestDto {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El valor debe ser un número positivo")
    private Double amount;

    @PositiveOrZero(message = "El porcentaje de descuento no puede ser negativo")
    private Integer discount;

    @NotNull(message = "El afiliado es obligatorio")
    private Long affiliateId;

    // TODO: reemplazar por SecurityContext/JWT cuando exista AuthService
    @NotNull(message = "El empleado creador es obligatorio")
    private Long creatorId;

    // TODO: reemplazar por SecurityContext/JWT cuando exista AuthService
    @NotNull(message = "La delegación es obligatoria")
    private Delegation delegation;

    @Valid
    @NotNull(message = "Los datos de la orden médica son obligatorios")
    private MedicalOrderRequestDto medicalOrderDto;
}