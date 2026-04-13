package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.ThingLookupResponse;
import com.isums.houseservice.domains.emuns.DeviceType;
import com.isums.houseservice.domains.entities.InstalledDevice;
import com.isums.houseservice.infrastructures.mappers.InstalledDeviceMapper;
import com.isums.houseservice.infrastructures.repositories.InstalledDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstalledDeviceServiceImpl")
class InstalledDeviceServiceImplTest {

    @Mock private InstalledDeviceRepository installedDeviceRepository;
    @Mock private InstalledDeviceMapper installedDeviceMapper;

    @InjectMocks private InstalledDeviceServiceImpl service;

    private InstalledDevice device;
    private ThingLookupResponse dto;
    private final String thingName = "thing-123";

    @BeforeEach
    void setUp() {
        device = InstalledDevice.builder()
                .id(UUID.randomUUID())
                .houseId(UUID.randomUUID())
                .thingName(thingName)
                .type(DeviceType.ELECTRIC)
                .isActive(true)
                .healthStatus("OK")
                .build();
        dto = new ThingLookupResponse(thingName, device.getHouseId(), device.getAssetId(),
                DeviceType.ELECTRIC, true, "OK");
    }

    @Nested
    @DisplayName("lookupThing")
    class LookupThing {

        @Test
        @DisplayName("onlyActive=true uses active-only query and returns mapped dto")
        void activeOnly() {
            when(installedDeviceRepository.findByThingNameAndIsActiveTrue(thingName))
                    .thenReturn(Optional.of(device));
            when(installedDeviceMapper.toLookupResponse(device)).thenReturn(dto);

            assertThat(service.lookupThing(thingName, true)).isSameAs(dto);

            verify(installedDeviceRepository).findByThingNameAndIsActiveTrue(thingName);
        }

        @Test
        @DisplayName("onlyActive=false uses unfiltered query")
        void unfiltered() {
            when(installedDeviceRepository.findByThingName(thingName)).thenReturn(Optional.of(device));
            when(installedDeviceMapper.toLookupResponse(device)).thenReturn(dto);

            assertThat(service.lookupThing(thingName, false)).isSameAs(dto);
            verify(installedDeviceRepository).findByThingName(thingName);
        }

        @Test
        @DisplayName("throws 404 ResponseStatusException when device missing (active-only)")
        void notFoundActive() {
            when(installedDeviceRepository.findByThingNameAndIsActiveTrue(thingName))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.lookupThing(thingName, true))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verifyNoInteractions(installedDeviceMapper);
        }

        @Test
        @DisplayName("throws 404 ResponseStatusException when device missing (unfiltered)")
        void notFoundUnfiltered() {
            when(installedDeviceRepository.findByThingName(thingName))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.lookupThing(thingName, false))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
