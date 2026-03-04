package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.House;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {

    @EntityGraph(attributePaths = "functionalAreas")
    Optional<House> findWithFunctionalAreasById(UUID id);

    @EntityGraph(attributePaths = {"functionalAreas","functionalAreas.house"})
    List<House> findAll();
}
