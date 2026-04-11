package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.HouseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HouseImageRepository extends JpaRepository<HouseImage, UUID> {
    List<HouseImage> findByHouseId(UUID houseId);
    @Query("""
    SELECT hi FROM HouseImage hi
    JOIN FETCH hi.house h
    WHERE h.id IN :houseIds
""")
    List<HouseImage> findByHouseIds(@Param("houseIds") List<UUID> houseIds);
}
