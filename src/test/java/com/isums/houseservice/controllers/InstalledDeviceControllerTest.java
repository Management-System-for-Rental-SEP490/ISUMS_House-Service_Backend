package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.ThingLookupResponse;
import com.isums.houseservice.domains.emuns.DeviceType;
import com.isums.houseservice.exceptions.GlobalExceptionHandler;
import com.isums.houseservice.infrastructures.abstracts.InstalledDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstalledDeviceController")
class InstalledDeviceControllerTest {

    @Mock private InstalledDeviceService service;
    @InjectMocks private InstalledDeviceController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /things/{name} defaults onlyActive=true and returns payload")
    void defaultActive() throws Exception {
        ThingLookupResponse dto = new ThingLookupResponse(
                "thing-1", UUID.randomUUID(), UUID.randomUUID(),
                DeviceType.ELECTRIC, true, "OK");
        when(service.lookupThing("thing-1", true)).thenReturn(dto);

        mvc.perform(get("/api/houses/installed-devices/things/{name}", "thing-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thingName").value("thing-1"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET with onlyActive=false forwards the flag to service")
    void explicitFalse() throws Exception {
        ThingLookupResponse dto = new ThingLookupResponse(
                "thing-2", UUID.randomUUID(), UUID.randomUUID(),
                DeviceType.WATER, false, "DEGRADED");
        when(service.lookupThing("thing-2", false)).thenReturn(dto);

        mvc.perform(get("/api/houses/installed-devices/things/{name}", "thing-2")
                        .param("onlyActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("ResponseStatusException 404 surfaces as 404 via dedicated handler")
    void notFound() throws Exception {
        when(service.lookupThing("missing", true))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thing not found"));

        mvc.perform(get("/api/houses/installed-devices/things/{name}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Thing not found"));
    }
}
