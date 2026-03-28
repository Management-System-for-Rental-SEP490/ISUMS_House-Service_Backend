package com.isums.houseservice.infrastructures.grpcs;

import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.mappers.HouseGrpcMapper;
import com.isums.houseservice.grpc.*;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import com.isums.houseservice.infrastructures.repositories.RegionStaffRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseGrpcImpl extends HouseServiceGrpc.HouseServiceImplBase {
    private final HouseRepository houseRepository;
    private final RegionStaffRepository regionStaffRepository;

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

    @Override
    @Transactional(readOnly = true)
    public void getRegionIdByHouseId(GetHouseRequest request,
                                     StreamObserver<GetRegionResponse> responseObserver) {

        UUID houseId;

        try {
            houseId = UUID.fromString(request.getHouseId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("houseId is not valid UUID")
                            .asRuntimeException()
            );
            return;
        }

        try {
            UUID regionId = houseRepository.findRegionIdByHouseId(houseId)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("Region not found for house")
                            .asRuntimeException());

            GetRegionResponse res = GetRegionResponse.newBuilder()
                    .setRegionId(regionId.toString())
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getStaffByRegion(GetRegionRequest request, StreamObserver<GetStaffByRegionResponse> responseObserver) {
        try{
            UUID regionId;
            try{
                regionId = UUID.fromString(request.getRegionId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("region_id is not a valid UUID").asRuntimeException());
                return;
            }

            List<UUID> staffIds = regionStaffRepository.findByIdRegionId(regionId).stream()
                    .map(rs -> rs.getId().getStaffId())
                    .toList();

            GetStaffByRegionResponse res = GetStaffByRegionResponse.newBuilder()
                    .addAllStaffIds(staffIds.stream().map(UUID::toString).toList())
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
