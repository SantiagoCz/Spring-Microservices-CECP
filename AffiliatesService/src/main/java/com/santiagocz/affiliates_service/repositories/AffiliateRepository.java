package com.santiagocz.affiliates_service.repositories;

import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.domain.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliateRepository extends JpaRepository<Affiliate, Long> {

    Optional<Affiliate> findByDni(String dni);

    boolean existsByDni(String dni);

    Page<Affiliate> findAllByOrderByLastNameAsc(Pageable pageable);

    Page<Affiliate> findByAffiliateTypeOrderByLastNameAsc(AffiliateType affiliateType, Pageable pageable);

    Page<Affiliate> findByAffiliateTypeAndStatusOrderByLastNameAsc(AffiliateType affiliateType,
                                                                   Status status,
                                                                   Pageable pageable);


    List<Affiliate> findByPrimaryAffiliateIdOrderByLastNameAsc(Long primaryAffiliateId);

    long countByPrimaryAffiliateId(Long primaryAffiliateId);

    @Query("SELECT DISTINCT a FROM Affiliate a " +
            "LEFT JOIN FETCH a.familyMembers " +
            "WHERE a.id = :id AND a.affiliateType = 'PRIMARY'")
    Optional<Affiliate> findPrimaryWithFamilyById(@Param("id") Long id);

    @Query("SELECT a FROM Affiliate a " +
            "WHERE a.id = :primaryId OR a.primaryAffiliate.id = :primaryId " +
            "ORDER BY a.affiliateType DESC, a.lastName ASC")
    List<Affiliate> findFamilyGroupByPrimaryId(@Param("primaryId") Long primaryId);

    Page<Affiliate> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
}