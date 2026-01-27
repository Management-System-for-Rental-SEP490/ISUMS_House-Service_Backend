package com.isums.houseservice.services;

import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import jakarta.persistence.Timeout;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseQuery {
    private final HouseRepository houseRepository;

    @CacheEvict(allEntries = true, cacheNames = "allHouses")
    public House createHouse(House house) {
        return houseRepository.save(house);
    }

    @Cacheable(value = "allHouses", sync = true)
    public List<House> GetAllHouses() {
        return houseRepository.findAll();
    }
}
