package com.isums.houseservice.domains.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_members")
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
    private Tenant tenant;

    public UUID getUserId() {
        return id.getUserId();
    }

    @Column(name = "is_owner")
    private boolean isOwner;

    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
