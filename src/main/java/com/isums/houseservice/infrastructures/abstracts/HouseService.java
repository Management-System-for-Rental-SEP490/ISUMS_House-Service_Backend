package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.dtos.HouseAccessStatus;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.dtos.HouseHistoryItemDto;
import com.isums.houseservice.domains.dtos.HouseImageDto;
import com.isums.houseservice.domains.dtos.UpdateHouseTranslationsRequest;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.entities.HouseImage;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HouseService {
    HouseDto CreateHouse(CreateHouseRequest req);

    PageResponse<HouseDto> getAll(PageRequest request);

    HouseDto getHouseById(UUID id);

    List<HouseHistoryItemDto> getHouseHistory(UUID houseId);

    List<HouseDto> getHouseByUserId(String userId);

    void uploadHouseImages(UUID houseId, List<MultipartFile> file);

    void deleteHouseImage(UUID houseId, UUID imageId);

    void activeHouseForUser(UUID userId, UUID houseId, Instant handoverDate);

    List<HouseDto> getHousesByRegionId(UUID regionId);

    List<HouseAccessStatus> getMyHouseAccess(String keycloakId);

    void revokeHouseAccessForUser(UUID tenantId, UUID houseId);

    void deactivateHouseForUser(UUID tenantId, UUID houseId, boolean keepUnavailable);

    void openDepositWindow(UUID houseId);

    void closeDepositWindow(UUID houseId);

    void releaseExpiredDeposit(UUID tenantId, UUID houseId);

    void completeCheckoutAndHandover(
            UUID oldTenantId,
            UUID houseId,
            Instant effectiveAt,
            boolean demo);

    void setTenantAccessRestriction(UUID tenantId, UUID houseId, boolean restricted);

    HouseDto updateHouseTranslations(UUID houseId, UpdateHouseTranslationsRequest request);

    HouseDto updateHouse(UUID id, CreateHouseRequest req);

    HouseDto updateHouseStatus(UUID id, HouseStatus nextStatus, UUID actorId);

    void subscribeToAvailability(UUID houseId, String keycloakId);

    void unsubscribeFromAvailability(UUID houseId, String keycloakId);

    boolean isSubscribed(UUID houseId, String keycloakId);
}
