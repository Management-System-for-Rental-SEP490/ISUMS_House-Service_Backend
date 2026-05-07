package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.*;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.userservice.grpc.UserResponse;
import common.paginations.dtos.PageRequestParams;
import common.paginations.dtos.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;
    private final UserClientsGrpc userClientsGrpc;

    @GetMapping
    public ApiResponse<PageResponse<HouseDto>> GetAllHouses(@ParameterObject @Valid @ModelAttribute PageRequestParams params) {
        var res = houseService.getAll(params.toPageRequest());
        return ApiResponses.ok(res, "Success to get all houses");
    }

    @PostMapping
    @Operation(
            summary = "Tao nha moi voi ho tro da ngu tu dong",
            description = """
                        Manager / landlord tao nha:
                        - Cac field van ban (name, address, ward, commune, city, description) LUON o tieng Viet.
                        - Neu KHONG gui *Translations -> AWS Translate tu dong sinh en + ja tu tieng Viet.
                        - Neu gui *Translations voi 1 hoac nhieu locale (en, ja) -> ban dich do duoc GIU NGUYEN,
                          AWS chi dich cho locale con thieu.
                        - Example body:
                          ```json
                          {
                            "name": "Nha Hieu Quan 7",
                            "nameTranslations": { "en": "Hieu Mansion D7" },
                            "address": "123 Nguyen Van Linh",
                            "addressTranslations": { "en": "123 NVL", "ja": "123 NVL" },
                            "regionId": "...",
                            "ward": "P. Tan Phong",
                            "commune": "",
                            "city": "TP. HCM",
                            "description": "Nha 3 tang view song",
                            "numberOfFloors": 3,
                            "houseImages": []
                          }
                          ```
                        Response tra ve *Translations day du 3 ngon ngu cho FE show cho admin review.
                    """
    )
    public ApiResponse<HouseDto> CreateHouse(@RequestBody CreateHouseRequest req) {
        HouseDto res = houseService.CreateHouse(req);
        return ApiResponses.created(res, "Success to create house");
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update house information",
            description = """
                    Partial update of the legal-info house fields (areaM2, structure,
                    numberOfFloors, description, …). Name/address use a shared endpoint
                    here; individual translations use PATCH /{id}/translations.
                    A null field means keep the current value.
                    """
    )
    public ApiResponse<HouseDto> UpdateHouse(
            @PathVariable UUID id,
            @RequestBody CreateHouseRequest req
    ) {
        HouseDto res = houseService.updateHouse(id, req);
        return ApiResponses.ok(res, "Success to update house");
    }

    @PostMapping("/{id}/subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> subscribe(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        houseService.subscribeToAvailability(id, jwt.getSubject());
        return ApiResponses.ok(null, "Subscribed");
    }

    @DeleteMapping("/{id}/subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> unsubscribe(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        houseService.unsubscribeFromAvailability(id, jwt.getSubject());
        return ApiResponses.ok(null, "Unsubscribed");
    }

    @GetMapping("/{id}/subscriptions/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Boolean> isSubscribed(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponses.ok(houseService.isSubscribed(id, jwt.getSubject()), "Success");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER')")
    @Operation(
            summary = "Manually update house operational status",
            description = "Allowed transitions: AVAILABLE <-> REPAIRED. RENTED is driven by the contract lifecycle."
    )
    public ApiResponse<HouseDto> updateHouseStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateHouseStatusRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        HouseDto res = houseService.updateHouseStatus(id, req.status(), actorId);
        return ApiResponses.ok(res, "House status updated");
    }

    @PatchMapping("/{id}/translations")
    @Operation(
            summary = "Cap nhat translations cho 1 nha",
            description = """
                        Cho phep manager/landlord sua rieng tung ngon ngu (vi/en/ja) cho moi field.
                        Body chi can chua field + ngon ngu muon sua, cac field/ngon ngu khong gui se duoc GIU NGUYEN.

                        Vi du: chi can sua description tieng Nhat:
                        ```json
                        {
                          "descriptionTranslations": { "ja": "new japanese text" }
                        }
                        ```
                        Neu patch chua khoa vi, gia tri vi cung duoc dong bo vao cot goc (name/address/...).
                    """
    )
    public ApiResponse<HouseDto> updateHouseTranslations(
            @PathVariable UUID id,
            @RequestBody UpdateHouseTranslationsRequest req
    ) {
        HouseDto res = houseService.updateHouseTranslations(id, req);
        return ApiResponses.ok(res, "Success to update house translations");
    }

    @GetMapping("/{id}")
    public ApiResponse<HouseDto> getHouseById(@PathVariable UUID id) {
        HouseDto house = houseService.getHouseById(id);
        return ApiResponses.ok(house, "Success to get house by id");
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<HouseHistoryItemDto>> getHouseHistory(@PathVariable UUID id) {
        List<HouseHistoryItemDto> res = houseService.getHouseHistory(id);
        return ApiResponses.ok(res, "Success to get house history");
    }

    @GetMapping("/house")
    public ApiResponse<List<HouseDto>> getMyHouses(@AuthenticationPrincipal Jwt jwt) {
        List<HouseDto> houses = houseService.getHouseByUserId(jwt.getSubject());
        return ApiResponses.ok(houses, "Get houses successfully");
    }

    @PostMapping(value = "/{houseId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HouseDto> uploadHouseImages(@PathVariable UUID houseId, @RequestParam("files") List<MultipartFile> files) {
        houseService.uploadHouseImages(houseId, files);
        return ApiResponses.ok(null, "Upload images successfully");
    }

    @DeleteMapping("{houseId}/image/{imageId}")
    public ApiResponse<Void> deleteHouseImage(@PathVariable UUID houseId, @PathVariable UUID imageId) {
        houseService.deleteHouseImage(houseId, imageId);
        return ApiResponses.ok(null, "Delete image successfully");
    }

    @GetMapping("/my-access")
    @PreAuthorize("hasRole('TENANT')")
    @Operation(
            summary = "Tenant-group house-access status",
            description = """
                        Returns the houses rented by the tenant group with their status:
                        - ACCESSIBLE: may enter the house
                        - PENDING_HANDOVER: before handover date
                        - PENDING_DEPOSIT: deposit not paid
                        - PENDING_FIRST_RENT: first-month rent unpaid (entry still allowed, warning shown)

                        The FE uses this response to:
                        1. If a single house → enter directly, show warnings if any
                        2. If multiple houses → show the house picker screen
                        3. If hasUnpaidInvoice → show a banner linking to the payment page
                    """
    )
    public ApiResponse<List<HouseAccessStatus>> myAccess(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<HouseAccessStatus> statuses = houseService.getMyHouseAccess(keycloakId);

        return ApiResponses.ok(statuses, "Success");
    }

    @GetMapping("/region/{regionId}")
    public ApiResponse<List<HouseDto>> getHousesByRegionId(@PathVariable UUID regionId) {
        List<HouseDto> res = houseService.getHousesByRegionId(regionId);
        return ApiResponses.ok(res, "Success");
    }

}

