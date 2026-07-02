package com.santiagocz.medical_coverage_service.dto.payment;

import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentUpdateDto {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El valor debe ser un número positivo")
    private Double amount;

    @PositiveOrZero(message = "El porcentaje de descuento no puede ser negativo")
    private Integer discount;

    @Valid
    @NotNull(message = "Los datos de la orden médica son obligatorios")
    private MedicalOrderRequestDto medicalOrderDto;
}