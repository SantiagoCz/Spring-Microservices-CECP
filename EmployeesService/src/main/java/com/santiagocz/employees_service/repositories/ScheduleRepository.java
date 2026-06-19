package com.santiagocz.employees_service.repositories;

import com.santiagocz.employees_service.domain.entities.Schedule;
import com.santiagocz.employees_service.domain.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s JOIN FETCH s.employee WHERE s.employee.id = :employeeId")
    List<Schedule> findByEmployeeIdFetchEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.employee WHERE s.dayOfWeek = :dayOfWeek")
    List<Schedule> findByDayOfWeekFetchEmployee(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.employee.id = :employeeId " +
            "AND s.dayOfWeek = :day AND s.status = :status " +
            "AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId) " +
            "AND (s.startTime < :end AND s.endTime > :start)")
    boolean existsOverlappingSchedule(
            @Param("employeeId") Long employeeId,
            @Param("day") DayOfWeek day,
            @Param("status") EmployeeStatus status,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("excludeScheduleId") Long excludeScheduleId
    );
}