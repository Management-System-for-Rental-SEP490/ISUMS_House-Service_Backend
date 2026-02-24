package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.ThingLookupResponse;
import com.isums.houseservice.infrastructures.abstracts.InstalledDeviceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/installed-devices")
@RequiredArgsConstructor
public class InstalledDeviceController {

    private final InstalledDeviceService installedDeviceService;

    @GetMapping("/things/{thingName}")
    public ApiResponse<ThingLookupResponse> lookupThing(@PathVariable @NotBlank String thingName, @RequestParam(defaultValue = "true") boolean onlyActive) {

        var res = installedDeviceService.lookupThing(thingName, onlyActive);
        return ApiResponses.ok(res, "Lookup thing successfully");
    }
}
