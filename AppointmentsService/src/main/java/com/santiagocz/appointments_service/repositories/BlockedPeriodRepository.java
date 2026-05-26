package com.santiagocz.appointments_service.repositories;

import com.santiagocz.appointments_service.domain.entities.BlockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, Long> {

    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b " +
            "WHERE (b.professional IS NULL OR b.professional.id = :professionalId) " +
            "AND b.startDate <= :date AND b.endDate >= :date")
    boolean existsBlockOnDate(@Param("professionalId") Long professionalId,
                              @Param("date") LocalDate date);

    @Query("SELECT COUNT(b) > 0 FROM BlockedPeriod b " +
            "WHERE ((:professionalId IS NULL AND b.professional IS NULL) " +
            "    OR b.professional.id = :professionalId) " +
            "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    boolean existsOverlappingBlock(@Param("professionalId") Long professionalId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

}