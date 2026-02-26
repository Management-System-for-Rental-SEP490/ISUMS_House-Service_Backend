package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.House;
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

    @Override
    public House CreateHouse(CreateHouseRequest req) {
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

            return houseRepository.save(house);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    //@Cacheable(value = "allHouses")
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
    public House getHouseById(UUID id) {
        try {

            return houseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("House not found"));
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by id: " + ex.getMessage());
        }
    }
}
