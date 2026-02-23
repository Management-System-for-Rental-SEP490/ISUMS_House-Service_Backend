package com.isums.houseservice.infrastructures.grpcs;

import com.google.protobuf.util.Timestamps;
import com.isums.houseservice.grpc.GetTenantByUserIdRequest;
import com.isums.houseservice.grpc.TenantResponse;
import com.isums.houseservice.grpc.TenantServiceGrpc;
import com.isums.houseservice.infrastructures.abstracts.TenantService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantGrpcImpl extends TenantServiceGrpc.TenantServiceImplBase {

    private final TenantService tenantService;

    @Override
    public void getTenantByUserId(GetTenantByUserIdRequest request, StreamObserver<TenantResponse> responseObserver) {

        var tenant = tenantService.getTenantByUserId(UUID.fromString(request.getUserId()));

        TenantResponse response = TenantResponse.newBuilder()
                .setId(tenant.id().toString())
                .setIsActive(tenant.isActive())
                .setCreatedAt(Timestamps.fromMillis(tenant.createdAt().toEpochMilli()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}


