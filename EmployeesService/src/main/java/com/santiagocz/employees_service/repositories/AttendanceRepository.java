package com.santiagocz.employees_service.repositories;

import com.santiagocz.employees_service.domain.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.id = :id")
    Optional<Attendance> findByIdFetchEmployee(@Param("id") Long id);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.employee.id = :employeeId")
    List<Attendance> findByEmployeeIdFetchEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee " +
            "WHERE a.employee.id = :employeeId AND a.checkIn BETWEEN :from AND :to")
    List<Attendance> findByEmployeeIdAndCheckInBetweenFetchEmployee(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee " +
            "WHERE a.checkOut IS NULL AND a.checkIn < :startOfToday")
    List<Attendance> findOpenCheckInsBeforeToday(@Param("startOfToday") LocalDateTime startOfToday);

    boolean existsByEmployeeIdAndCheckOutIsNullAndCheckInBetween(
            Long employeeId,
            LocalDateTime from,
            LocalDateTime to);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.id = :employeeId " +
            "AND a.checkOut IS NULL " +
            "AND a.checkIn BETWEEN :from AND :to " +
            "ORDER BY a.checkIn DESC")
    Optional<Attendance> findOpenCheckInToday(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}