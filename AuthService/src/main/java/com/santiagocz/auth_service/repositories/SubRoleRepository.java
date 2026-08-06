package com.santiagocz.auth_service.repositories;

import com.santiagocz.auth_service.domain.entities.SubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubRoleRepository extends JpaRepository<SubRole, Long> {

    Optional<SubRole> findByName(String name);

    boolean existsByName(String name);
}