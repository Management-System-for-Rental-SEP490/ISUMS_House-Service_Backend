package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.HouseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HouseImageRepository extends JpaRepository<HouseImage, UUID> {
    List<HouseImage> findByHouseId(UUID houseId);
}
