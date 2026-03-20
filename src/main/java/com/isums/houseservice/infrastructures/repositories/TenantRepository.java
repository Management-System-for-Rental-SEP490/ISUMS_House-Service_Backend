package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    @Query("""
   select tm.tenant
   from TenantMember tm
   where tm.id.userId = :userId
""")
    Optional<Tenant> findTenantByUserId(UUID userId);
}
