package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.dtos.HouseImageDto;
import com.isums.houseservice.domains.entities.HouseImage;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.kafkas.HouseEventProducer;
import com.isums.houseservice.infrastructures.mappers.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.HouseImageRepository;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import com.isums.houseservice.infrastructures.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final HouseMapper houseMapper;
    private final RegionRepository regionRepository;
    private final HouseEventProducer houseEventProducer;
    private final S3ServiceImpl s3;
    private final HouseImageRepository houseImageRepository;

    @Override
    public HouseDto CreateHouse(CreateHouseRequest req) {
        Region region = regionRepository.findById(req.regionId())
                .orElseThrow(() -> new RuntimeException("Region not found"));

        House house = House.builder()
                .name(req.name())
                .address(req.address())
                .region(region)
                .ward(req.ward())
                .commune(req.commune())
                .city(req.city())
                .description(req.description())
                .status(HouseStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        House created = houseRepository.save(house);
        houseEventProducer.publishHouseCreated(created.getId());
        return houseMapper.toDto(created);
    }

    @Override
    @Cacheable(value = "allHouses")
    @Transactional(readOnly = true)
    public List<HouseDto> GetAllHouses() {
        try {

            List<House> houses = houseRepository.findAll();
            return houseMapper.toDtos(houses);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get all houses");
        }
    }

    @Override
    @Cacheable(value = "houseInformation", key = "#id")
    @Transactional(readOnly = true)
    public HouseDto getHouseById(UUID id) {
        try {
            House house = houseRepository.findWithFunctionalAreasById(id)
                    .orElseThrow(() -> new RuntimeException("House not found"));

            return houseMapper.toDto(house);
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by id: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<HouseDto> getHousesByUser(UUID userId) {
        try {
            List<House> houses = houseRepository.findByUserRentalId(userId);
            return houseMapper.toDtos(houses);
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by user id: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void uploadHouseImages(UUID houseId, List<MultipartFile> files) {
        boolean isExist = houseRepository.existsById(houseId);
        if (!isExist) {
            throw new NotFoundException("House not found: " + houseId);
        }

        House house = houseRepository.getReferenceById(houseId);

        files.forEach(file -> {
            String key = s3.upload(file, "houses/" + houseId);

            HouseImage image = HouseImage.builder()
                    .house(house)
                    .key(key)
                    .build();

            houseImageRepository.save(image);
        });
    }

    @Override
    @Cacheable(value = "houseImages", key = "#houseId")
    public List<HouseImageDto> getHouseImages(UUID houseId) {
        List<HouseImage> images = houseImageRepository.findByHouseId(houseId);

        List<HouseImageDto> imagesDto = new ArrayList<>();
        images.forEach(image -> {
            String url = s3.getImageUrl(image.getKey());
            imagesDto.add(new HouseImageDto(image.getId(), url, image.getCreatedAt()));
        });

        return imagesDto;
    }

    @Override
    @CacheEvict(value = "houseImages", key = "#houseId", allEntries = true)
    public void deleteHouseImage(UUID houseId, UUID imageId) {
        HouseImage image = houseImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("House image not found"));
        s3.delete(image.getKey());
        houseImageRepository.delete(image);
    }
}
