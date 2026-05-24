package com.santiagocz.appointments_service.components;

import com.santiagocz.appointments_service.domain.entities.Appointment;
import com.santiagocz.appointments_service.domain.enums.AppointmentStatus;
import com.santiagocz.appointments_service.repositories.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private final AppointmentRepository appointmentRepository;

    private static final Set<AppointmentStatus> PENDING_STATUSES =
            Set.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    // Recordatorio: todos los días 9:00, avisa los turnos de mañana sin confirmar
    @Scheduled(cron = "0 0 9 * * *")
    public void sendReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> pending = appointmentRepository.findByStatusAndStartDateTimeBetween(
                AppointmentStatus.SCHEDULED, tomorrow.atStartOfDay(), tomorrow.atTime(LocalTime.MAX));

        for (Appointment appointment : pending) {
            // TODO: envío por WhatsApp (el asistente / API de notificaciones)
            // notificationService.sendReminder(appointment);
        }
    }

    // Ausentes: cada hora, los turnos vencidos que nadie cerró pasan a NO_SHOW
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void markNoShows() {
        List<Appointment> expired = appointmentRepository.findByStatusInAndEndDateTimeBefore(
                PENDING_STATUSES, LocalDateTime.now());

        for (Appointment appointment : expired) {
            appointment.setStatus(AppointmentStatus.NO_SHOW);
        }
    }
}