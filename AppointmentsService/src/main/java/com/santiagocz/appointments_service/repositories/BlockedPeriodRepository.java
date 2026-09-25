package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.BlockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, Long> {

    // ¿Hay algún bloqueo que pise esta franja? (feriados + bloqueos del profesional)
    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b " +
            "WHERE (b.professional IS NULL OR b.professional.id = :professionalId) " +
            "AND b.startDate <= :date AND b.endDate >= :date " +
            "AND (b.startTime IS NULL " +
            "     OR (b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsBlockInSlot(@Param("professionalId") Long professionalId,
                              @Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    // ¿Este bloqueo se superpone con otro ya existente? (excludedId: el propio, al editar)
    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b " +
            "WHERE ((:professionalId IS NULL AND b.professional IS NULL) " +
            "    OR b.professional.id = :professionalId) " +
            "AND b.startDate <= :endDate AND b.endDate >= :startDate " +
            "AND (b.startTime IS NULL OR :startTime IS NULL " +
            "     OR (b.startTime < :endTime AND b.endTime > :startTime)) " +
            "AND (:excludedId IS NULL OR b.id <> :excludedId)")
    boolean existsOverlappingBlock(@Param("professionalId") Long professionalId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime,
                                   @Param("excludedId") Long excludedId);

    @Query("SELECT b FROM BlockedPeriod b " +
            "WHERE (b.professional IS NULL OR b.professional.id = :professionalId) " +
            "AND b.endDate >= :fromDate " +
            "ORDER BY b.startDate ASC")
    List<BlockedPeriod> findUpcomingForProfessional(@Param("professionalId") Long professionalId,
                                                    @Param("fromDate") LocalDate fromDate);
}