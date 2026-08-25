package com.santiagocz.common.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Auditable {

    @CreatedBy
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "userId", column = @Column(name = "created_by_user_id", updatable = false)),
            @AttributeOverride(name = "fullName", column = @Column(name = "created_by_name", updatable = false))
    })
    private Auditor createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "userId", column = @Column(name = "updated_by_user_id")),
            @AttributeOverride(name = "fullName", column = @Column(name = "updated_by_name"))
    })
    private Auditor updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}