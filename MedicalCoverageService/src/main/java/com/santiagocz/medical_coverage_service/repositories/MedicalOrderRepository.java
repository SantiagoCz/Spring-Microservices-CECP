package com.santiagocz.medical_coverage_service.repositories;

import com.santiagocz.common.delegation.Delegation;
import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
            "FROM MedicalOrder m JOIN m.payment p " +
            "WHERE m.number = :number " +
            "AND m.status = :status " +
            "AND p.delegation = :delegation")
    boolean existsByNumberAndStatusAndDelegation(@Param("number") Long number,
                                                 @Param("status") Status status,
                                                 @Param("delegation") Delegation delegation);
}