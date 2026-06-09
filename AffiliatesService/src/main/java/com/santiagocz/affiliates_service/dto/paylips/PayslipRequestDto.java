package com.santiagocz.affiliates_service.dto.paylips;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PayslipRequestDto {

    @NotNull(message = "El ID del afiliado es obligatorio")
    private Long affiliateId;

    @NotNull(message = "El período es obligatorio")
    @PastOrPresent(message = "El período no puede ser futuro")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate period;
}