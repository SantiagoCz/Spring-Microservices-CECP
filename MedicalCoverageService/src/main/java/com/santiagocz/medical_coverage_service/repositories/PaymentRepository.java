package com.santiagocz.medical_coverage_service.repositories;

import com.santiagocz.common.delegation.Delegation;
import com.santiagocz.medical_coverage_service.domain.entities.Payment;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder " +
            "WHERE p.affiliateId = :affiliateId ORDER BY p.date DESC")
    List<Payment> findByAffiliateId(@Param("affiliateId") Long affiliateId);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder mo " +
            "WHERE p.date BETWEEN :startDate AND :endDate " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:delegation IS NULL OR p.delegation = :delegation) " +
            "AND (:createdBy IS NULL OR p.createdBy = :createdBy) " +
            "ORDER BY p.date DESC, mo.number ASC")
    List<Payment> findByFilters(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("status") Status status,
                                @Param("delegation") Delegation delegation,
                                @Param("createdBy") Long createdBy);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.medicalOrder mo " +
            "WHERE mo.number = :orderNumber")
    Optional<Payment> findByMedicalOrderNumber(@Param("orderNumber") Long orderNumber);
}