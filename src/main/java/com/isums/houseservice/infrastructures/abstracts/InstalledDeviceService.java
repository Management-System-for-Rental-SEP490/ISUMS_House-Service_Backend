package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.ThingLookupResponse;

public interface InstalledDeviceService {
    ThingLookupResponse lookupThing(String thingName, boolean onlyActive);
}
