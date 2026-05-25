package com.santiagocz.appointments_service.dto.blockePeriod;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BlockedPeriodRequestDto {

    // null = feriado
    private Long professionalId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser pasada")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @FutureOrPresent(message = "La fecha de fin no puede ser pasada")
    private LocalDate endDate;

    @NotBlank(message = "El motivo es obligatorio")
    private String reason;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la de inicio")
    private boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate); // endDate >= startDate (permite un solo día)
    }
}
