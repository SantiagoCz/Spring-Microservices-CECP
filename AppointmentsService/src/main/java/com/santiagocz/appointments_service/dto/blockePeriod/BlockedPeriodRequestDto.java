package com.santiagocz.appointments_service.dto.blockePeriod;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

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

    private LocalTime startTime;

    private LocalTime endTime;

    @NotBlank(message = "El motivo es obligatorio")
    private String reason;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la de inicio")
    private boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate); // endDate >= startDate (permite un solo día)
    }

    @AssertTrue(message = "Debe indicar hora de inicio y de fin, o ninguna de las dos")
    private boolean isTimeRangeComplete() {
        return (startTime == null) == (endTime == null);
    }

    @AssertTrue(message = "La hora de fin debe ser posterior a la de inicio")
    private boolean isEndTimeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}
