package com.isums.houseservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_groups",
        indexes = @Index(name = "idx_tenant_group_house", columnList = "house_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantGroup {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}