package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.dtos.HouseImageDto;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.entities.HouseImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface HouseService {
    HouseDto CreateHouse(CreateHouseRequest req);
    List<HouseDto> GetAllHouses();
    HouseDto getHouseById(UUID id);
    List<HouseDto> getHouseByUserId(String userId);
    void uploadHouseImages(UUID houseId, List<MultipartFile> file);
    List<HouseImageDto> getHouseImages(UUID houseId);
    void deleteHouseImage(UUID houseId, UUID imageId);
}
