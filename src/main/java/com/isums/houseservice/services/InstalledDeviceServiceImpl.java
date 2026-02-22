package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.ThingLookupResponse;
import com.isums.houseservice.domains.entities.InstalledDevice;
import com.isums.houseservice.infrastructures.abstracts.InstalledDeviceService;
import com.isums.houseservice.infrastructures.mappers.InstalledDeviceMapper;
import com.isums.houseservice.infrastructures.repositories.InstalledDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InstalledDeviceServiceImpl implements InstalledDeviceService {

    private final InstalledDeviceRepository installedDeviceRepository;
    private final InstalledDeviceMapper installedDeviceMapper;

    @Override
    public ThingLookupResponse lookupThing(String thingName, boolean onlyActive) {

        InstalledDevice device = (onlyActive
                ? installedDeviceRepository.findByThingNameAndIsActiveTrue(thingName)
                : installedDeviceRepository.findByThingName(thingName))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thing not found"));

        return installedDeviceMapper.toLookupResponse(device);
    }
}
