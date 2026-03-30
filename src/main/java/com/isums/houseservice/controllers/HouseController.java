package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.*;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.userservice.grpc.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
        List<HouseDto> houses = houseService.getHouseByUserId(jwt.getSubject());
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

    @GetMapping("/my-access")
    @PreAuthorize("hasRole('TENANT')")
    @Operation(
            summary = "Trạng thái truy cập nhà của tenant",
            description = """
                        Trả về danh sách nhà tenant được thuê kèm trạng thái:
                        - ACCESSIBLE: có thể vào nhà
                        - PENDING_HANDOVER: chưa đến ngày nhận nhà
                        - PENDING_DEPOSIT: chưa thanh toán tiền cọc
                        - PENDING_FIRST_RENT: chưa thanh toán tiền thuê tháng đầu (vẫn vào được, có cảnh báo)
                    
                        FE dùng response này để:
                        1. Nếu 1 nhà → vào thẳng, hiện cảnh báo nếu có
                        2. Nếu nhiều nhà → hiện màn hình chọn nhà
                        3. Nếu hasUnpaidInvoice → hiện banner dẫn đến trang thanh toán
                    """
    )
    public ApiResponse<List<HouseAccessStatus>> myAccess(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getSubject());
        List<HouseAccessStatus> statuses = houseService.getMyHouseAccess(tenantId);

        return ApiResponses.ok(statuses, "Success");
    }
}
