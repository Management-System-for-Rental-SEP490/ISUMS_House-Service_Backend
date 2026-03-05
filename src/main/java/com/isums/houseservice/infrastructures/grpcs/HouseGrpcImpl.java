package com.isums.houseservice.infrastructures.grpcs;

import com.isums.houseservice.infrastructures.mappers.HouseGrpcMapper;
import com.isums.houseservice.grpc.*;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseGrpcImpl extends HouseServiceGrpc.HouseServiceImplBase {
    private final HouseRepository houseRepository;

    @Override
    @Transactional(readOnly = true)
    public void getHouseById(GetHouseRequest request, StreamObserver<HouseResponse> responseObserver) {
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

            HouseResponse res = HouseGrpcMapper.toHouseResponse(house.get());

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(ex).asRuntimeException()
            );
        }
    }
}
