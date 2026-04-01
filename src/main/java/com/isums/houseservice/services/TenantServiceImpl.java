package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.TenantDto;
import com.isums.houseservice.domains.entities.TenantGroup;
import com.isums.houseservice.infrastructures.abstracts.TenantService;
import com.isums.houseservice.infrastructures.mappers.TenantMapper;
import com.isums.houseservice.infrastructures.repositories.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Override
    public TenantDto getTenantByUserId(UUID userId) {
        TenantGroup tenantGroup = tenantRepository.findTenantByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        return tenantMapper.toTenantDto(tenantGroup);
    }
}
