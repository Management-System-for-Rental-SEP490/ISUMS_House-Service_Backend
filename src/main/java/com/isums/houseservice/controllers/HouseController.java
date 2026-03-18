package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.*;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.entities.House;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @GetMapping
    public ApiResponse<List<HouseDto>> GetAllHouses() {
        List<HouseDto> res = houseService.GetAllHouses();
        return ApiResponses.ok(res, "Success to get all houses");
    }

    @PostMapping
    public ApiResponse<HouseDto> CreateHouse(@RequestBody CreateHouseRequest req) {
        HouseDto res = houseService.CreateHouse(req);
        return ApiResponses.created(res, "Success to create house");
    }

    @GetMapping("/{id}")
    public ApiResponse<HouseDto> getHouseById(@PathVariable UUID id) {
        HouseDto house = houseService.getHouseById(id);
        return ApiResponses.ok(house, "Success to get house by id");
    }

    @GetMapping("/house")
    public ApiResponse<List<HouseDto>> getMyHouses(@AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        List<HouseDto> houses = houseService.getHousesByUser(userId);

        return ApiResponses.ok(houses, "Get houses successfully");
    }

    @PostMapping(value = "/{houseId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HouseDto> uploadHouseImages(@PathVariable UUID houseId, @RequestParam("files") List<MultipartFile> files) {
        houseService.uploadHouseImages(houseId, files);
        return ApiResponses.ok(null, "Upload images successfully");
    }

    @GetMapping("{houseId}/images")
    public ApiResponse<List<HouseImageDto>> getHouseImages(@PathVariable UUID houseId) {
        List<HouseImageDto> images = houseService.getHouseImages(houseId);
        return ApiResponses.ok(images, "Get images successfully");
    }

    @DeleteMapping("{houseId}/image/{imageId}")
    public ApiResponse<Void> deleteHouseImage(@PathVariable UUID houseId, @PathVariable UUID imageId) {
        houseService.deleteHouseImage(houseId, imageId);
        return ApiResponses.ok(null, "Delete image successfully");
    }
}
