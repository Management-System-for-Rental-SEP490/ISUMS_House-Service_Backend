package com.isums.houseservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.exceptions.GlobalExceptionHandler;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.abstracts.FunctionalAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FunctionalAreaController")
class FunctionalAreaControllerTest {

    @Mock private FunctionalAreaService functionalAreaService;

    @InjectMocks private FunctionalAreaController controller;

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private FunctionalAreaDto dto(UUID id, UUID houseId) {
        return new FunctionalAreaDto(id, houseId, "Kitchen", AreaType.KITCHEN,
                "1", "desc", FuctionalAreaStatus.NORMAL, Instant.now(), Instant.now(), null);
    }

    @Test
    @DisplayName("POST creates area and returns 200")
    void create() throws Exception {
        UUID houseId = UUID.randomUUID();
        CreateFunctionalAreaRequest req = new CreateFunctionalAreaRequest(
                houseId, "Kitchen", AreaType.KITCHEN, "1", "desc");
        when(functionalAreaService.createArea(any())).thenReturn(dto(UUID.randomUUID(), houseId));

        mvc.perform(post("/api/houses/functionalAreas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Kitchen"));
    }

    @Test
    @DisplayName("GET by houseId returns list")
    void getAll() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(functionalAreaService.getAllAreas(houseId))
                .thenReturn(List.of(dto(UUID.randomUUID(), houseId)));

        mvc.perform(get("/api/houses/functionalAreas/{houseId}", houseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Kitchen"));
    }

    @Test
    @DisplayName("PUT updates area")
    void update() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateFunctionalAreaRequest req = new UpdateFunctionalAreaRequest(
                "New", null, null, null, null, null);
        when(functionalAreaService.updateArea(eq(id), any()))
                .thenReturn(dto(id, UUID.randomUUID()));

        mvc.perform(put("/api/houses/functionalAreas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(functionalAreaService).updateArea(eq(id), any());
    }

    @Test
    @DisplayName("DELETE returns 200 with boolean true")
    void delete_() throws Exception {
        UUID id = UUID.randomUUID();
        when(functionalAreaService.deleteArea(id)).thenReturn(true);

        mvc.perform(delete("/api/houses/functionalAreas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("PUT returns 404 when service throws NotFoundException")
    void updateNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateFunctionalAreaRequest req = new UpdateFunctionalAreaRequest(
                "X", null, null, null, null, null);
        when(functionalAreaService.updateArea(eq(id), any()))
                .thenThrow(new NotFoundException("Functional area not found: " + id));

        mvc.perform(put("/api/houses/functionalAreas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET returns 404 when service throws NotFoundException")
    void notFound() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(functionalAreaService.getAllAreas(houseId)).thenThrow(new NotFoundException("missing"));

        mvc.perform(get("/api/houses/functionalAreas/{houseId}", houseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }
}
