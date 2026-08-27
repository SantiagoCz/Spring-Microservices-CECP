package com.santiagocz.auth_service.domain.entities;

import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_hierarchy_role", columnList = "hierarchy_role")
})
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "subroles", "person"})
@EqualsAndHashCode(exclude = {"subroles", "person"})
public class User implements UserDetails {

    // ──────────── IDENTITY ────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    // ──────────── ROLES & RELATIONS ────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "hierarchy_role", nullable = false)
    private HierarchyRole hierarchyRole;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @BatchSize(size = 20)
    @JoinTable(
            name = "user_subroles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "subrol_id")
    )
    @Builder.Default
    private Set<SubRole> subroles = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "person_id", unique = true)
    private Person person;

    // ──────────── ACCOUNT STATE ────────────

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    // ──────────── AUDIT ────────────

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ──────────── LIFECYCLE ────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ──────────── USER DETAILS ────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + hierarchyRole.name()));
        subroles.forEach(subrole ->
                authorities.add(new SimpleGrantedAuthority("SUB_" + subrole.getName()))
        );
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}