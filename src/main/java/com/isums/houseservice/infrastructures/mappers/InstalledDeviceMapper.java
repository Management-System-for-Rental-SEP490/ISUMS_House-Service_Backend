package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.ThingLookupResponse;
import com.isums.houseservice.domains.entities.InstalledDevice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstalledDeviceMapper {

    ThingLookupResponse toLookupResponse(InstalledDevice device);
}
