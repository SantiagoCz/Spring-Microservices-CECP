package com.santiagocz.auth_service.repositories;

import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@PathVariable Long id);

    @EntityGraph(attributePaths = "person")
    Page<User> findByCreatedBy(Long createdBy, Pageable pageable);

    Page<User> findByHierarchyRoleNot(HierarchyRole hierarchyRole, Pageable pageable);

    @Query(value = "SELECT * FROM users u WHERE u.deleted_at IS NULL AND (" +
            "u.created_by = :adminId OR u.created_by IN " +
            "(SELECT c.id FROM users c WHERE c.created_by = :adminId))",
            countQuery = "SELECT count(*) FROM users u WHERE u.deleted_at IS NULL AND (" +
                    "u.created_by = :adminId OR u.created_by IN " +
                    "(SELECT c.id FROM users c WHERE c.created_by = :adminId))",
            nativeQuery = true)
    Page<User> findSubtreeOf(@Param("adminId") Long adminId, Pageable pageable);
}