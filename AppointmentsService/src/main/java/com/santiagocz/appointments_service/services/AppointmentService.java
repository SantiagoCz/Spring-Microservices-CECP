package com.santiagocz.appointments_service.services;

import com.santiagocz.appointments_service.clients.AffiliateClient;
import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.entities.Patient;
import com.santiagocz.appointments_service.domain.entities.Professional;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import com.santiagocz.appointments_service.domain.enums.AppointmentType;
import com.santiagocz.appointments_service.domain.enums.Status;
import com.santiagocz.appointments_service.dto.appointment.AppointmentRequestDto;
import com.santiagocz.appointments_service.dto.appointment.AppointmentResponseDto;
import com.santiagocz.appointments_service.exceptions.EntityConflictException;
import com.santiagocz.appointments_service.exceptions.EntityNotFoundException;
import com.santiagocz.appointments_service.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final ProfessionalRepository professionalRepository;
    private final PatientRepository patientRepository;
    private final ScheduleRepository scheduleRepository;
    private final AffiliationLookupService affiliationLookupService;

    // Estados que NO ocupan un lugar en la agenda
    private static final Set<AppointmentStatus> NON_OCCUPYING_STATUSES =
            Set.of(AppointmentStatus.CANCELED, AppointmentStatus.DELETED);

    // ──────────── CREATE ────────────

    @Transactional
    public AppointmentResponseDto create(AppointmentRequestDto dto) {
        Professional professional = getActiveProfessional(dto.getProfessionalId());
        Patient patient = getActivePatient(dto.getPatientId());

        LocalDateTime start = dto.getStartDateTime();
        LocalDateTime end = start.plusMinutes(dto.getDurationMinutes());

        // 1. El turno tiene que caer dentro de un horario en que el profesional atiende
        validateWithinSchedule(professional.getId(), start, end);
        // 2. Verificar que no sea feriado, vacaciones, etc.
        validateDateAvailable(professional.getId(), start.toLocalDate());
        // 3. Capacidad: solo se valida para REGULAR; el sobreturno la saltea
        if (dto.getType() == AppointmentType.REGULAR) {
            validateCapacity(professional, start, end, null);
        }

        Appointment appointment = Appointment.builder()
                .professional(professional)
                .patient(patient)
                .startDateTime(start)
                .endDateTime(end)
                .status(AppointmentStatus.SCHEDULED)
                .type(dto.getType())
                .build();

        return buildResponseDto(appointmentRepository.save(appointment));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public AppointmentResponseDto findById(Long id) {
        return buildResponseDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByProfessionalId(Long professionalId) {
        return appointmentRepository.findByProfessionalId(professionalId)
                .stream().map(this::buildResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream().map(this::buildResponseDto).toList();
    }

//    @Transactional(readOnly = true)
//    public List<AppointmentResponseDto> findProfessionalAgenda(Long professionalId, LocalDate date) {
//        LocalDateTime from = date.atStartOfDay();
//        LocalDateTime to = date.atTime(LocalTime.MAX);
//        return appointmentRepository
//                .findByProfessionalIdAndStartDateTimeBetween(professionalId, from, to)
//                .stream().map(this::buildResponseDto).toList();
//    }

    public List<AppointmentResponseDto> findProfessionalAgenda(Long professionalId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        List<Appointment> appointments = appointmentRepository
                .findByProfessionalIdAndStartDateTimeBetween(professionalId, from, to);

        if (appointments.isEmpty()) return List.of();

        List<String> dnis = appointments.stream()
                .map(a -> a.getPatient().getDni())
                .distinct()
                .toList();

        Optional<Set<String>> activeDnis = affiliationLookupService.activeDnisFromBatch(dnis);

        return appointments.stream()
                .map(a -> buildResponseDto(
                        a,
                        activeDnis.map(set -> set.contains(a.getPatient().getDni())).orElse(null)
                ))
                .toList();
    }

    // ──────────── UPDATE ────────────

    @Transactional
    public AppointmentResponseDto update(Long id, AppointmentRequestDto dto) {
        Appointment appointment = getEntityById(id);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new EntityConflictException("Solo se puede reprogramar un turno programado o confirmado");
        }

        Professional professional = getActiveProfessional(appointment.getProfessional().getId());

        LocalDateTime start = dto.getStartDateTime();
        LocalDateTime end = start.plusMinutes(dto.getDurationMinutes());

        validateWithinSchedule(professional.getId(), start, end);
        validateDateAvailable(professional.getId(), start.toLocalDate());

        if (dto.getType() == AppointmentType.REGULAR) {
            validateCapacity(professional, start, end, appointment.getId());
        }

        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setType(dto.getType());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return buildResponseDto(appointment);
    }

    // ──────────── STATUS ────────────

    @Transactional
    public void confirm(Long id) {
        Appointment appointment = getEntityById(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new EntityConflictException("Solo se puede confirmar un turno programado");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
    }

    @Transactional
    public void cancel(Long id) {
        Appointment appointment = getEntityById(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new EntityConflictException("Solo se puede cancelar un turno programado o confirmado");
        }
        appointment.setStatus(AppointmentStatus.CANCELED);
    }

    @Transactional
    public void markAttended(Long id) {
        Appointment appointment = getEntityById(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new EntityConflictException("Solo se puede marcar atendido un turno programado o confirmado");
        }
        appointment.setStatus(AppointmentStatus.ATTENDED);
    }

    // ──────────── PRIVATES ────────────

    private Appointment getEntityById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el turno con ID: " + id));
    }

    private Professional getActiveProfessional(Long professionalId) {
        Professional professional = professionalRepository.findByIdWithLock(professionalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el profesional con ID: " + professionalId));
        if (professional.getStatus() != Status.ACTIVE) {
            throw new EntityConflictException(
                    "El profesional con ID " + professionalId + " no está activo");
        }
        return professional;
    }

    private Patient getActivePatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el paciente con ID: " + patientId));
        if (patient.getStatus() != Status.ACTIVE) {
            throw new EntityConflictException(
                    "El paciente con ID " + patientId + " no está activo");
        }
        return patient;
    }

    private void validateWithinSchedule(Long professionalId, LocalDateTime start, LocalDateTime end) {
        boolean covered = scheduleRepository.existsCoveringSchedule(
                professionalId,
                start.getDayOfWeek(),
                Status.ACTIVE,
                start.toLocalTime(),
                end.toLocalTime());
        if (!covered) {
            throw new EntityConflictException("El profesional no atiende en ese horario");
        }
    }

    private void validateCapacity(Professional professional, LocalDateTime start,
                                  LocalDateTime end, Long excludeAppointmentId) {
        long occupied = appointmentRepository.countOverlapping(
                professional.getId(), start, end, NON_OCCUPYING_STATUSES, excludeAppointmentId);
        if (occupied >= professional.getSlotCapacity()) {
            throw new EntityConflictException(
                    "El profesional ya tiene la agenda completa en ese horario");
        }
    }

    private void validateDateAvailable(Long professionalId, LocalDate date) {
        if (blockedPeriodRepository.existsBlockOnDate(professionalId, date)) {
            throw new EntityConflictException(
                    "El profesional no atiende ese día.");
        }
    }

    // Mappers
    private AppointmentResponseDto buildResponseDto(Appointment appointment) {
        Professional professional = appointment.getProfessional();
        Patient patient = appointment.getPatient();
        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .professionalId(professional.getId())
                .professionalName(professional.getFirstName() + " " + professional.getLastName())
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .startDateTime(appointment.getStartDateTime())
                .endDateTime(appointment.getEndDateTime())
                .status(appointment.getStatus().getDisplayName())
                .type(appointment.getType().getDisplayName())
                .build();
    }

    private AppointmentResponseDto buildResponseDto(Appointment appointment, Boolean affiliated) {
        Professional professional = appointment.getProfessional();
        Patient patient = appointment.getPatient();
        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .professionalId(professional.getId())
                .professionalName(professional.getFirstName() + " " + professional.getLastName())
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .startDateTime(appointment.getStartDateTime())
                .endDateTime(appointment.getEndDateTime())
                .status(appointment.getStatus().getDisplayName())
                .type(appointment.getType().getDisplayName())
                .patientAffiliated(affiliated)
                .build();
    }
}