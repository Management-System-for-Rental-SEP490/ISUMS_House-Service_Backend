package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.TenantMember;
import com.isums.houseservice.domains.entities.TenantMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TenantMemberRepository extends JpaRepository<TenantMember, TenantMemberId> {

    List<TenantMember> findByTenantGroupId(UUID tenantGroupId);

    @Query("""
        SELECT COUNT(m) > 0 FROM TenantMember m
        WHERE m.id.tenantId = :tenantGroupId
        AND m.id.userId = :userId
    """)
    boolean existsByTenantGroupIdAndUserId(UUID tenantGroupId, UUID userId);
}