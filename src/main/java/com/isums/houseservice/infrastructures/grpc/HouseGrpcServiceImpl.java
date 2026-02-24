//package com.isums.houseservice.infrastructures.grpc;
//
//
//import com.isums.houseservice.domains.entities.House;
//import com.isums.houseservice.grpc.GetHouseRequest;
//import com.isums.houseservice.grpc.HouseResponse;
//import com.isums.houseservice.grpc.HouseServiceGrpc;
//import com.isums.houseservice.infrastructures.repositories.HouseRepository;
//import io.grpc.Status;
//import io.grpc.stub.StreamObserver;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class HouseGrpcServiceImpl extends HouseServiceGrpc.HouseServiceImplBase {
//    private final HouseRepository houseRepository;
//
//    @Override
//    public void getHouseById(GetHouseRequest request, StreamObserver<HouseResponse> responseObserver) {
//        HouseResponse response;
//        try {
//
//            UUID houseId = UUID.fromString(request.getId());
//            House house = houseRepository.findById(houseId)
//                    .orElseThrow(() -> Status.NOT_FOUND
//                            .withDescription("House not found: " + request.getId())
//                            .asRuntimeException());
//
//            response = HouseResponse.newBuilder()
//                    .setId(house.getId().toString())
//                    .setName(house.getName())
//                    .build();
//
//            responseObserver.onNext(response);
//            responseObserver.onCompleted();
//        } catch (IllegalArgumentException e) {
//            responseObserver.onError(Status.INVALID_ARGUMENT
//                    .withDescription("Invalid UUID: " + request.getId())
//                    .asRuntimeException());
//        } catch (Exception e) {
//            responseObserver.onError(Status.INTERNAL
//                    .withDescription("Internal error")
//                    .withCause(e)
//                    .asRuntimeException());
//        }
//    }
//}
