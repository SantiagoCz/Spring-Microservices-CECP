package com.santiagocz.medical_coverage_service.repositories;

import com.santiagocz.medical_coverage_service.domain.entities.Payment;
import com.santiagocz.medical_coverage_service.domain.enums.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder " +
            "WHERE p.date >= :startOfMonth AND p.date < :startOfNextMonth " +
            "ORDER BY p.date DESC")
    List<Payment> findAllThisMonth(@Param("startOfMonth") LocalDate startOfMonth,
                                   @Param("startOfNextMonth") LocalDate startOfNextMonth);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder mo " +
            "WHERE p.creatorId = :creatorId " +
            "AND p.date >= :startOfMonth AND p.date < :startOfNextMonth " +
            "ORDER BY p.date DESC, mo.number DESC")
    List<Payment> findByCreatorIdThisMonth(@Param("creatorId") Long creatorId,
                                           @Param("startOfMonth") LocalDate startOfMonth,
                                           @Param("startOfNextMonth") LocalDate startOfNextMonth);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder mo " +
            "WHERE p.delegation = :delegation " +
            "AND p.date >= :startOfMonth AND p.date < :startOfNextMonth " +
            "ORDER BY p.date DESC, mo.number DESC")
    List<Payment> findByDelegationThisMonth(@Param("delegation") Delegation delegation,
                                            @Param("startOfMonth") LocalDate startOfMonth,
                                            @Param("startOfNextMonth") LocalDate startOfNextMonth);

    Optional<Payment> findByMedicalOrderId(Long medicalOrderId);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder " +
            "WHERE p.affiliateId = :affiliateId ORDER BY p.date DESC")
    List<Payment> findByAffiliateId(@Param("affiliateId") Long affiliateId);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder mo " +
            "WHERE mo.number = :orderNumber")
    Optional<Payment> findByMedicalOrderNumber(@Param("orderNumber") Long orderNumber);

    @Query("SELECT p FROM Payment p WHERE p.date = :date ORDER BY p.date DESC")
    List<Payment> findByDate(@Param("date") LocalDate date);
}