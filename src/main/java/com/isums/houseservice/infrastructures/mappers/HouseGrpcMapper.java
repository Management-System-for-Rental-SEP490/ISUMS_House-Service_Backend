package com.isums.houseservice.infrastructures.mappers;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import common.i18n.TranslationMap;
import com.isums.houseservice.domains.entities.FunctionalArea;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.grpc.*;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@NoArgsConstructor
public class HouseGrpcMapper {

    public HouseResponse toHouseResponse(House house) {
        HouseResponse.Builder builder = HouseResponse.newBuilder()
                .setId(uuidToString(house.getId()))
                .setUserRentalId(uuidToString(house.getUserRentalId()))
                .setName(nullToEmpty(resolveVi(house.getName(), house.getNameTranslations())))
                .setAddress(nullToEmpty(resolveVi(house.getAddress(), house.getAddressTranslations())))
                .setCommune(nullToEmpty(resolveVi(house.getCommune(), house.getCommuneTranslations())))
                .setCity(nullToEmpty(resolveVi(house.getCity(), house.getCityTranslations())))
                .setDescription(nullToEmpty(resolveVi(house.getDescription(), house.getDescriptionTranslations())))
                .setStatus(mapHouseStatus(house.getStatus()))
                .setCreatedAt(toTimestamp(house.getCreatedAt()))
                .setUpdatedAt(toTimestamp(house.getUpdatedAt()));

        if (house.getFunctionalAreas() != null) {
            for (FunctionalArea fa : house.getFunctionalAreas()) {
                builder.addFunctionalAreas(toFunctionalAreaResponse(fa));
            }
        }

        if (house.getRegion() != null) {
            builder.setRegionId(uuidToString(house.getRegion().getId()));
        }
        return builder.build();
    }

    private FunctionalAreaResponse toFunctionalAreaResponse(FunctionalArea fa) {
        return FunctionalAreaResponse.newBuilder()
                .setId(uuidToString(fa.getId()))
                .setName(nullToEmpty(resolveVi(fa.getName(), fa.getNameTranslations())))
                .setAreaType(mapAreaType(fa.getAreaType()))
                .setFloorNo(nullToEmpty(fa.getFloorNo()))
                .setDescription(nullToEmpty(resolveVi(fa.getDescription(), fa.getDescriptionTranslations())))
                .setStatus(mapFunctionalAreaStatus(fa.getStatus()))
                .setCreatedAt(toTimestamp(fa.getCreatedAt()))
                .setUpdatedAt(toTimestamp(fa.getUpdatedAt()))
                .build();
    }

    private String resolveVi(String source, TranslationMap translations) {
        if (translations == null || translations.getTranslations().isEmpty()) {
            return source;
        }
        String resolved = translations.getTranslations().get("vi");
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String uuidToString(java.util.UUID id) {
        return id == null ? "" : id.toString();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? Timestamp.getDefaultInstance()
                : Timestamps.fromMillis(instant.toEpochMilli());
    }

    private HouseStatus mapHouseStatus(com.isums.houseservice.domains.emuns.HouseStatus s) {
        if (s == null) return HouseStatus.HOUSE_STATUS_UNSPECIFIED;
        return switch (s) {
            case AVAILABLE -> HouseStatus.HOUSE_STATUS_AVAILABLE;
            case RENTED -> HouseStatus.HOUSE_STATUS_RENTED;
            case REPAIRED -> HouseStatus.HOUSE_STATUS_REPAIRED;
        };
    }

    private AreaType mapAreaType(com.isums.houseservice.domains.emuns.AreaType t) {
        if (t == null) return AreaType.AREA_TYPE_UNSPECIFIED;
        return switch (t) {
            case KITCHEN -> AreaType.AREA_TYPE_KITCHEN;
            case HALLWAY -> AreaType.AREA_TYPE_HALLWAY;
            case BEDROOM -> AreaType.AREA_TYPE_BEDROOM;
            case BATHROOM -> AreaType.AREA_TYPE_BATHROOM;
            case LIVINGROOM -> AreaType.AREA_TYPE_LIVINGROOM;
            case GARDEN -> AreaType.AREA_TYPE_GARDEN;
            case ALL -> AreaType.AREA_TYPE_ALL;
            case OTHER -> AreaType.AREA_TYPE_OTHER;
        };
    }

    private FunctionalAreaStatus mapFunctionalAreaStatus(com.isums.houseservice.domains.emuns.FuctionalAreaStatus s) {
        if (s == null) return FunctionalAreaStatus.FUNCTIONAL_AREA_STATUS_UNSPECIFIED;
        return switch (s) {
            case REPAIRED -> FunctionalAreaStatus.FUNCTIONAL_AREA_STATUS_REPAIRED;
            case UNAVAILABLE -> FunctionalAreaStatus.FUNCTIONAL_AREA_STATUS_UNAVAILABLE;
            case NORMAL -> FunctionalAreaStatus.FUNCTIONAL_AREA_STATUS_NORMAL;
        };
    }
}
