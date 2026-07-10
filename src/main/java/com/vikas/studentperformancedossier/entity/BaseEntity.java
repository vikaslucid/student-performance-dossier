package com.vikas.studentperformancedossier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// @MappedSuperclass: fields are mapped into each subclass's own table instead of this class having a table of its own.
@MappedSuperclass
// @EntityListeners: hooks Spring Data's auditing listener into JPA lifecycle callbacks so it can populate the @CreatedDate/@LastModifiedDate fields below.
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // @Id: marks this field as the primary key.
    @Id
    // @GeneratedValue: delegates primary key generation to the database (IDENTITY = auto-increment column), rather than assigning it in code.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @CreatedDate: Spring Data auditing sets this once, when the entity is first persisted.
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // @LastModifiedDate: Spring Data auditing updates this every time the entity is saved.
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
