package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.Schedule;
import com.santiagocz.appointments_service.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByProfessionalId(Long professionalId);

    List<Schedule> findByDayOfWeek(DayOfWeek dayOfWeek);

    @Query("SELECT COUNT(s) > 0 FROM Schedule s " +
            "WHERE s.professional.id = :professionalId " +
            "AND s.dayOfWeek = :dayOfWeek " +
            "AND s.status = :status " +
            "AND s.startTime < :endTime " +
            "AND s.endTime > :startTime " +
            "AND (:scheduleId IS NULL OR s.id <> :scheduleId)")
    boolean existsOverlappingSchedule(
            @Param("professionalId") Long professionalId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("status") Status status,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(s) > 0 FROM Schedule s " +
            "WHERE s.professional.id = :professionalId " +
            "AND s.dayOfWeek = :dayOfWeek " +
            "AND s.status = :status " +
            "AND s.startTime <= :startTime " +
            "AND s.endTime >= :endTime")
    boolean existsCoveringSchedule(
            @Param("professionalId") Long professionalId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("status") Status status,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}