package com.isums.houseservice.infrastructures.grpcs;

import com.isums.houseservice.grpc.*;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetGrpcImpl extends AssetGrpcServiceGrpc.AssetGrpcServiceImplBase {
    private final HouseRepository houseRepository;

    @Override
    @Transactional(readOnly = true)
    public void getAssetItemByHouseId(GetAssetItemByHouseIdRequest request, StreamObserver<AssetItemResponse> responseObserver) {
        try {
            UUID houseId;
            try {
                houseId = UUID.fromString(request.getHouseId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("house_id is not a valid UUID").asRuntimeException());
                return;
            }

            var house = houseRepository.findWithFunctionalAreasById(houseId);
            if (house.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND.withDescription("House not found: " + houseId).asRuntimeException()
                );
                return;
            }

            AssetItemResponse res = AssetItemResponse.newBuilder()
                    .addAllAssetItems(house.get().getFunctionalAreas().stream()
                            .map(fa -> AssetItem.newBuilder()
                                    .setId(fa.getId().toString())
                                    .setName(fa.getName())
                                    .setDescription(fa.getDescription() != null ? fa.getDescription() : "")
                                    .setStatus(fa.getStatus() != null ? fa.getStatus().name() : "")
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(ex).asRuntimeException()
            );
        }
    }
}
