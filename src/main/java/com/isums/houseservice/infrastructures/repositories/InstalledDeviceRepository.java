package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.InstalledDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstalledDeviceRepository extends JpaRepository<InstalledDevice, UUID> {
    Optional<InstalledDevice> findByThingNameAndIsActiveTrue(String thingName);
    Optional<InstalledDevice> findByThingName(String thingName);
    List<InstalledDevice> findAllByHouseIdAndIsActiveTrue(UUID houseId);
}
