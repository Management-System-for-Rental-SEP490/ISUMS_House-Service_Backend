package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.TenantGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantGroup, UUID> {

    @Query("""
   select tm.tenantGroup
   from TenantMember tm
   where tm.id.userId = :userId
""")
    Optional<TenantGroup> findTenantByUserId(UUID userId);
}
