package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.TenantDto;
import com.isums.houseservice.domains.entities.TenantGroup;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantDto toTenantDto(TenantGroup tenantGroup);
}
