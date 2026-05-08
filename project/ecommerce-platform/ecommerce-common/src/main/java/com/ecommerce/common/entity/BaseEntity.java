package com.ecommerce.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /**
     * Automatically set to NOW() when the entity is first persisted.
     * @Column(updatable = false) ensures Hibernate never issues an UPDATE for this field.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated to NOW() every time the entity is updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * ID of the user who created this record.
     * Set by AuditorAware bean (reads from RequestContext.getUserId()).
     * Null for system-generated records.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    /**
     * ID of the user who last modified this record.
     * Automatically updated on each save.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Called by JPA before first persist (INSERT).
     * Generates UUID id if not already set (allows tests to pre-set IDs).
     * Sets version to 0 for new entities.
     */
    @PrePersist
    protected void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }
}
