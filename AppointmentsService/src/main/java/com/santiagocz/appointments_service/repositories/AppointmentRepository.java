package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Listados básicos
    List<Appointment> findByProfessionalId(Long professionalId);
    List<Appointment> findByPatientId(Long patientId);

    // Agenda de un profesional en un rango (un día, una semana)
    List<Appointment> findByProfessionalIdAndStartDateTimeBetween(
            Long professionalId, LocalDateTime from, LocalDateTime to);

    // Capacidad/overlap: cuántos turnos ocupan la ventana, sin contar los que no ocupan slot
    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.professional.id = :professionalId " +
            "AND a.startDateTime < :endDateTime " +
            "AND a.endDateTime > :startDateTime " +
            "AND a.status NOT IN :excludedStatuses " +
            "AND (:appointmentId IS NULL OR a.id <> :appointmentId)")
    long countOverlapping(
            @Param("professionalId") Long professionalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses,
            @Param("appointmentId") Long appointmentId);

    // Job de recordatorios: turnos de un estado que arrancan en una ventana (ej: los de mañana)
    List<Appointment> findByStatusAndStartDateTimeBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    // Job de ausentes: turnos vencidos que quedaron sin cerrar
    List<Appointment> findByStatusInAndEndDateTimeBefore(
            Collection<AppointmentStatus> statuses, LocalDateTime now);
}