package com.isums.houseservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_members",
        indexes = @Index(name = "idx_tenant_member_user", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMember {

    @EmbeddedId
    private TenantMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tenantId")
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantGroup tenantGroup;

    @Column(name = "is_owner", nullable = false)
    private boolean isOwner;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public UUID getUserId() {
        return id.getUserId();
    }

    public UUID getTenantGroupId() {
        return id.getTenantId();
    }
}