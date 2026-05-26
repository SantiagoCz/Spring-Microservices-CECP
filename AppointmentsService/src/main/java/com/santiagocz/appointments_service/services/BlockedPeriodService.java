package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.entities.BlockedPeriod;
import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodRequestDto;
import com.santiagocz.appointments_service.exceptions.EntityConflictException;
import com.santiagocz.appointments_service.exceptions.EntityNotFoundException;
import com.santiagocz.appointments_service.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlockedPeriodService {

    private final BlockedPeriodRepository blockedPeriodRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;

    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            Set.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    @Transactional
    public int blockPeriod(BlockedPeriodRequestDto dto) {

        Long professionalId = dto.getProfessionalId();
        LocalDate startDate = dto.getStartDate();
        LocalDate endDate = dto.getEndDate();
        String reason = dto.getReason();

        Professional professional = (professionalId == null) ? null
                : professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el profesional con ID: " + professionalId));

        if (blockedPeriodRepository.existsOverlappingBlock(professionalId, startDate, endDate)) {
            throw new EntityConflictException(
                    "Ya existe un bloqueo que se superpone con ese período");
        }

        blockedPeriodRepository.save(BlockedPeriod.builder()
                .professional(professional)
                .startDate(startDate)
                .endDate(endDate)
                .reason(reason)
                .build());

        List<Appointment> affected = appointmentRepository.findActiveInRange(
                professionalId, ACTIVE_STATUSES,
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        for (Appointment appointment : affected) {
            appointment.setStatus(AppointmentStatus.CANCELED);
            // TODO: avisar al paciente del turno cancelado (WhatsApp / asistente)
        }

        return affected.size();
    }
}