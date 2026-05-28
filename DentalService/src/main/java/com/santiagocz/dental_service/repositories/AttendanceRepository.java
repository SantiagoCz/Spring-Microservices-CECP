package com.santiagocz.dental_service.repositories;

import com.santiagocz.dental_service.domain.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByProfessionalIdAndDateBetween(
            Long professionalId,
            LocalDate from,
            LocalDate to
    );
}