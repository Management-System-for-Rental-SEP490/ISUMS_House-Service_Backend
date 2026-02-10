package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.FunctionalArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FunctionalAreaRepository extends JpaRepository<FunctionalArea, UUID> {
}
