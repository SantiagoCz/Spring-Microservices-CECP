package com.santiagocz.medical_coverage_service.repositories;

import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {

    @Query("SELECT m FROM MedicalOrder m LEFT JOIN FETCH m.payment p " +
            "WHERE m.number = :number AND m.status = :status")
    Optional<MedicalOrder> findByNumberAndStatus(@Param("number") Long number,
                                                 @Param("status") Status status);

    boolean existsByNumberAndStatus(Long number, Status status);
}