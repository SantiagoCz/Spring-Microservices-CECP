package com.santiagocz.affiliates_service.repositories;

import com.santiagocz.affiliates_service.domain.entities.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    boolean existsByPrimaryAffiliate_IdAndPeriod(Long primaryAffiliateId, LocalDate period);

    Optional<Payslip> findTopByPrimaryAffiliate_IdOrderByPeriodDesc(Long affiliateId);

    List<Payslip> findAllByPrimaryAffiliate_IdOrderByPeriodDesc(Long affiliateId);
}