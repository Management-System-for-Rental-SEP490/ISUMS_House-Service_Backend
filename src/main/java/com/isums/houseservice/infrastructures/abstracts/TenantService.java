package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.TenantDto;
import com.isums.houseservice.grpc.GetTenantByUserIdRequest;

import java.util.UUID;

public interface TenantService {

    public TenantDto getTenantByUserId(UUID userId);
}
