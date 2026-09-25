package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.entities.BlockedPeriod;
import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodRequestDto;
import com.santiagocz.appointments_service.dto.blockePeriod.BlockedPeriodResponseDto;
import com.santiagocz.appointments_service.repositories.*;
import com.santiagocz.common.exceptions.EntityConflictException;
import com.santiagocz.common.exceptions.EntityNotFoundException;
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

    // ──────────── CREATE ────────────

    @Transactional
    public BlockedPeriodResponseDto create(BlockedPeriodRequestDto dto) {
        Professional professional = resolveProfessional(dto.getProfessionalId());

        validateNoOverlap(dto.getProfessionalId(), dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(), null);

        BlockedPeriod blockedPeriod = blockedPeriodRepository.save(buildEntity(dto, professional));

        return buildResponseDto(blockedPeriod, cancelAffectedAppointments(blockedPeriod));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public BlockedPeriodResponseDto findById(Long id) {
        return buildResponseDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<BlockedPeriodResponseDto> findUpcomingForProfessional(Long professionalId) {
        return blockedPeriodRepository
                .findUpcomingForProfessional(professionalId, LocalDate.now())
                .stream().map(this::buildResponseDto).toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public BlockedPeriodResponseDto update(Long id, BlockedPeriodRequestDto dto) {
        BlockedPeriod blockedPeriod = getEntityById(id);
        Professional professional = resolveProfessional(dto.getProfessionalId());

        validateNoOverlap(dto.getProfessionalId(), dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(), id);

        blockedPeriod.setProfessional(professional);
        blockedPeriod.setStartDate(dto.getStartDate());
        blockedPeriod.setEndDate(dto.getEndDate());
        blockedPeriod.setStartTime(dto.getStartTime());
        blockedPeriod.setEndTime(dto.getEndTime());
        blockedPeriod.setReason(dto.getReason());

        return buildResponseDto(blockedPeriod, cancelAffectedAppointments(blockedPeriod));
    }

    @Transactional
    public BlockedPeriodResponseDto extend(Long id, LocalDate newEndDate) {
        BlockedPeriod blockPeriod = getEntityById(id);
        LocalDate currentEnd = blockPeriod.getEndDate();

        if (currentEnd.isBefore(LocalDate.now())) {
            throw new EntityConflictException(
                    "El período ya finalizó; creá uno nuevo");
        }
        if (!newEndDate.isAfter(currentEnd)) {
            throw new EntityConflictException(
                    "La nueva fecha de fin debe ser posterior a la actual (" + currentEnd + ")");
        }

        LocalDate tailStart = currentEnd.plusDays(1);
        Long professionalId = (blockPeriod.getProfessional() == null) ? null : blockPeriod.getProfessional().getId();

        validateNoOverlap(professionalId, tailStart, newEndDate,
                blockPeriod.getStartTime(), blockPeriod.getEndTime(), id);

        blockPeriod.setEndDate(newEndDate);

        return buildResponseDto(blockPeriod,
                cancelAffectedAppointments(blockPeriod, tailStart, newEndDate));
    }

    // ──────────── DELETE ────────────

    @Transactional
    public void delete(Long id) {
        BlockedPeriod blockedPeriod = getEntityById(id);
        blockedPeriodRepository.delete(blockedPeriod);
    }

    // ──────────── PRIVATES ────────────

    private BlockedPeriod getEntityById(Long id) {
        return blockedPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el periodo con ID: " + id));
    }

    // professionalId null = feriado: el bloqueo aplica a todos
    private Professional resolveProfessional(Long professionalId) {
        if (professionalId == null) {
            return null;
        }
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró al profesional con ID: " + professionalId));
    }

    private void validateNoOverlap(Long professionalId, LocalDate startDate, LocalDate endDate,
                                   LocalTime startTime, LocalTime endTime, Long excludedId) {
        if (blockedPeriodRepository.existsOverlappingBlock(
                professionalId, startDate, endDate, startTime, endTime, excludedId)) {
            throw new EntityConflictException(
                    "Ya existe un bloqueo que se superpone con ese período");
        }
    }

    // Cancela los turnos que caen dentro del bloqueo y devuelve cuántos fueron. (create y update)
    private int cancelAffectedAppointments(BlockedPeriod block) {
        return cancelAffectedAppointments(block, block.getStartDate(), block.getEndDate());
    }

    private int cancelAffectedAppointments(BlockedPeriod block, LocalDate from, LocalDate to) {
        Long professionalId = (block.getProfessional() == null)
                ? null
                : block.getProfessional().getId();

        List<Appointment> candidates = appointmentRepository.findActiveInRange(
                professionalId, ACTIVE_STATUSES,
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX));

        // Bloqueo de día completo: caen todos. Parcial: solo los que pisan la franja.
        List<Appointment> affected = (block.getStartTime() == null)
                ? candidates
                : candidates.stream()
                .filter(a -> a.getStartDateTime().toLocalTime().isBefore(block.getEndTime())
                        && a.getEndDateTime().toLocalTime().isAfter(block.getStartTime()))
                .toList();

        for (Appointment appointment : affected) {
            appointment.setStatus(AppointmentStatus.CANCELED);
            // TODO: avisar al paciente del turno cancelado (WhatsApp / asistente)
        }
        return affected.size();
    }

    // Mappers
    private BlockedPeriodResponseDto buildResponseDto(BlockedPeriod blockedPeriod) {
        return buildResponseDto(blockedPeriod, null);
    }

    private BlockedPeriodResponseDto buildResponseDto(BlockedPeriod blockedPeriod,
                                                      Integer totalAffectedAppointments) {
        Professional professional = blockedPeriod.getProfessional();
        return BlockedPeriodResponseDto.builder()
                .id(blockedPeriod.getId())
                .professionalId(professional == null ? null : professional.getId())
                .professionalName(professional == null ? null
                        : professional.getFirstName() + " " + professional.getLastName())
                .startDate(blockedPeriod.getStartDate())
                .endDate(blockedPeriod.getEndDate())
                .startTime(blockedPeriod.getStartTime())
                .endTime(blockedPeriod.getEndTime())
                .reason(blockedPeriod.getReason())
                .totalAffectedAppointments(totalAffectedAppointments)
                .build();
    }

    private BlockedPeriod buildEntity(BlockedPeriodRequestDto dto, Professional professional) {
        return BlockedPeriod.builder()
                .professional(professional)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reason(dto.getReason())
                .build();
    }

}