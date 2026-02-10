package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseQuery {
    private final HouseRepository houseRepository;
    private final HouseMapper houseMapper;

    @CacheEvict(allEntries = true, cacheNames = "allHouses")
    public House createHouse(House house) {
        return houseRepository.save(house);
    }

    //@Cacheable(value = "allHouses", sync = true)
    public List<HouseDto> GetAllHouses() {
        List<House> houses = houseRepository.findAll();
        return houseMapper.mapHouses(houses);
    }
}
