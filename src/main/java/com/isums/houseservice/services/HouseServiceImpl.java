package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.kafkas.HouseEventProducer;
import com.isums.houseservice.infrastructures.mappers.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import com.isums.houseservice.infrastructures.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final HouseMapper houseMapper;
    private final RegionRepository regionRepository;
    private final HouseEventProducer houseEventProducer;

    @Override
    public HouseDto CreateHouse(CreateHouseRequest req) {
        try {
            Region region = regionRepository.findById(req.regionId())
                    .orElseThrow(()-> new RuntimeException("Region not found"));

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

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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
        try{
            List<House> houses = houseRepository.findByUserRentalId(userId);
            return houseMapper.toDtos(houses);
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by user id: " + ex.getMessage());
        }
    }
}
