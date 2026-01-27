package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.House;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {
}
