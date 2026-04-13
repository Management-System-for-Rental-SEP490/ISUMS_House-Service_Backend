package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.TenantDto;
import com.isums.houseservice.domains.entities.TenantGroup;
import com.isums.houseservice.infrastructures.mappers.TenantMapper;
import com.isums.houseservice.infrastructures.repositories.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl")
class TenantServiceImplTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMapper tenantMapper;

    @InjectMocks private TenantServiceImpl service;

    @Test
    @DisplayName("getTenantByUserId returns mapped dto when tenant exists")
    void returnsDto() {
        UUID userId = UUID.randomUUID();
        TenantGroup group = TenantGroup.builder()
                .id(UUID.randomUUID()).houseId(UUID.randomUUID()).isActive(true).build();
        TenantDto dto = new TenantDto(group.getId(), true, Instant.now());

        when(tenantRepository.findTenantByUserId(userId)).thenReturn(Optional.of(group));
        when(tenantMapper.toTenantDto(group)).thenReturn(dto);

        assertThat(service.getTenantByUserId(userId)).isSameAs(dto);
    }

    @Test
    @DisplayName("getTenantByUserId throws when tenant missing")
    void throwsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(tenantRepository.findTenantByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTenantByUserId(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tenant not found");
    }
}
