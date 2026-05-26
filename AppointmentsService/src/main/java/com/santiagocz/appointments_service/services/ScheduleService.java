package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.entities.Schedule;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import com.santiagocz.appointments_service.domain.enums.Status;
import com.santiagocz.appointments_service.dto.schedule.ScheduleRequestDto;
import com.santiagocz.appointments_service.dto.schedule.ScheduleResponseDto;
import com.santiagocz.appointments_service.exceptions.EntityConflictException;
import com.santiagocz.appointments_service.exceptions.EntityNotFoundException;
import com.santiagocz.appointments_service.repositories.AppointmentRepository;
import com.santiagocz.appointments_service.repositories.ProfessionalRepository;
import com.santiagocz.appointments_service.repositories.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;

    private static final Set<AppointmentStatus> ACTIVE_APPOINTMENT_STATUSES =
            Set.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    // ──────────── CREATE ────────────

    @Transactional
    public ScheduleResponseDto create(ScheduleRequestDto dto) {
        Professional professional = professionalRepository.findById(dto.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el profesional con ID: " + dto.getProfessionalId()));

        if (professional.getStatus() != Status.ACTIVE) {
            throw new EntityConflictException(
                    "El profesional con ID " + dto.getProfessionalId() + " no está activo");
        }

        validateNoOverlap(dto.getProfessionalId(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), null);

        Schedule schedule = Schedule.builder()
                .professional(professional)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(Status.ACTIVE)
                .build();

        return buildResponseDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public List<ScheduleResponseDto> createBatch(List<ScheduleRequestDto> dtos) {
        return dtos.stream()
                .map(this::create)
                .collect(Collectors.toList());
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> findByProfessionalId(Long professionalId) {
        if (!professionalRepository.existsById(professionalId)) {
            throw new EntityNotFoundException(
                    "No se encontró el profesional con ID: " + professionalId);
        }
        return scheduleRepository.findByProfessionalId(professionalId)
                .stream()
                .map(this::buildResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> findActivesByProfessionalId(Long professionalId) {
        if (!professionalRepository.existsById(professionalId)) {
            throw new EntityNotFoundException(
                    "No se encontró el profesional con ID: " + professionalId);
        }
        return scheduleRepository.findByStatusAndProfessionalId(Status.ACTIVE, professionalId)
                .stream()
                .map(this::buildResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return scheduleRepository.findByDayOfWeek(dayOfWeek)
                .stream()
                .map(this::buildResponseDto)
                .collect(Collectors.toList());
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public ScheduleResponseDto update(Long id, ScheduleRequestDto dto) {
        Schedule schedule = getEntityById(id);

        if (schedule.getProfessional().getStatus() != Status.ACTIVE) {
            throw new EntityConflictException(
                    "No se puede editar el horario: el profesional está inactivo");
        }

        validateNoAppointmentsAffectedByChange(schedule, dto);

        validateNoOverlap(schedule.getProfessional().getId(), dto.getDayOfWeek(),
                dto.getStartTime(), dto.getEndTime(), schedule.getId());

        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        return buildResponseDto(scheduleRepository.save(schedule));
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void deactivate(Long id) {
        Schedule schedule = getEntityById(id);
        if (schedule.getStatus() == Status.INACTIVE) {
            throw new EntityConflictException("Horario ya inactivo");
        }
        validateNoActiveAppointments(schedule);
        schedule.setStatus(Status.INACTIVE);
    }

    // ──────────── PRIVATES ────────────

    private Schedule getEntityById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el horario con ID: " + id));
    }


    private void validateNoOverlap(Long professionalId, DayOfWeek day,
                                   LocalTime start, LocalTime end, Long excludeScheduleId) {
        if (scheduleRepository.existsOverlappingSchedule(
                professionalId, day, Status.ACTIVE, start, end, excludeScheduleId)) {
            String dia = day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            throw new EntityConflictException(
                    "El horario se superpone con otro existente el día " + dia);
        }
    }

    private void validateNoActiveAppointments(Schedule schedule) {
        List<Appointment> upcoming = appointmentRepository
                .findByProfessionalIdAndStatusInAndStartDateTimeAfter(
                        schedule.getProfessional().getId(),
                        ACTIVE_APPOINTMENT_STATUSES,
                        LocalDateTime.now());

        boolean hasActiveAppointments = upcoming.stream().anyMatch(a ->
                a.getStartDateTime().getDayOfWeek() == schedule.getDayOfWeek()
                        && a.getStartDateTime().toLocalTime().isBefore(schedule.getEndTime())
                        && a.getEndDateTime().toLocalTime().isAfter(schedule.getStartTime()));

        if (hasActiveAppointments) {
            throw new EntityConflictException(
                    "No se puede eliminar el horario: tiene turnos activos. Cancelalos primero.");
        }
    }

    private void validateNoAppointmentsAffectedByChange(Schedule schedule, ScheduleRequestDto dto) {
        List<Appointment> upcoming = appointmentRepository
                .findByProfessionalIdAndStatusInAndStartDateTimeAfter(
                        schedule.getProfessional().getId(),
                        ACTIVE_APPOINTMENT_STATUSES,
                        LocalDateTime.now());

        // Si cambia el día, no debe haber turnos en el día anterior
        if (dto.getDayOfWeek() != schedule.getDayOfWeek()) {
            boolean hasOnOldDay = upcoming.stream().anyMatch(a ->
                    a.getStartDateTime().getDayOfWeek() == schedule.getDayOfWeek()
                            && a.getStartDateTime().toLocalTime().isBefore(schedule.getEndTime())
                            && a.getEndDateTime().toLocalTime().isAfter(schedule.getStartTime()));

            if (hasOnOldDay) {
                throw new EntityConflictException(
                        "No se puede cambiar el día: hay turnos activos en el día actual. Cancelalos primero.");
            }
        }

        // Los turnos del nuevo día deben caber en el nuevo rango horario
        boolean hasAppointmentsOutside = upcoming.stream().anyMatch(a ->
                a.getStartDateTime().getDayOfWeek() == dto.getDayOfWeek()
                        && (a.getStartDateTime().toLocalTime().isBefore(dto.getStartTime())
                        || a.getEndDateTime().toLocalTime().isAfter(dto.getEndTime())));

        if (hasAppointmentsOutside) {
            throw new EntityConflictException(
                    "No se puede modificar el horario: hay turnos activos que quedarían fuera del nuevo rango. Cancelalos primero.");
        }
    }

    // Mapper
    private ScheduleResponseDto buildResponseDto(Schedule schedule) {
        Professional professional = schedule.getProfessional();
        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .professionalId(professional.getId())
                .professionalFirstName(professional.getFirstName())
                .professionalLastName(professional.getLastName())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .status(schedule.getStatus())
                .build();
    }
}