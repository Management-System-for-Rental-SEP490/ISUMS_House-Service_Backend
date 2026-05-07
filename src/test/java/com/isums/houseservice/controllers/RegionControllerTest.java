package com.isums.houseservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.exceptions.GlobalExceptionHandler;
import com.isums.houseservice.infrastructures.abstracts.RegionService;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.userservice.grpc.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionController")
class RegionControllerTest {

    @Mock private RegionService regionService;
    @Mock private UserClientsGrpc userClientsGrpc;

    @InjectMocks private RegionController controller;

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();
    private String keycloakId;

    @BeforeEach
    void setUp() {
        keycloakId = UUID.randomUUID().toString();
        Jwt jwt = Jwt.withTokenValue("t").header("alg","none").subject(keycloakId).build();

        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) {
                return Jwt.class.equals(p.getParameterType());
            }
            @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                                    NativeWebRequest w, WebDataBinderFactory b) {
                return jwt;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST / resolves managerId via gRPC and creates region")
    void create() throws Exception {
        UUID managerId = UUID.randomUUID();
        CreateRegionRequest req = new CreateRegionRequest("N", "d", managerId, List.of());
        UserResponse userResp = UserResponse.newBuilder().setId(managerId.toString()).build();
        RegionDto dto = new RegionDto(UUID.randomUUID(), "N", "d", managerId, List.of());

        when(userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId)).thenReturn(userResp);
        when(regionService.createRegion(eq(managerId.toString()), any())).thenReturn(dto);

        mvc.perform(post("/api/houses/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.name").value("N"));

        verify(userClientsGrpc).getUserIdAndRoleByKeyCloakId(keycloakId);
    }

    @Test
    @DisplayName("POST /{regionId}/staff/{staffId} assigns staff")
    void addStaff() throws Exception {
        UUID regionId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        RegionDto dto = new RegionDto(regionId, "N", "d", UUID.randomUUID(), List.of(staffId));
        when(regionService.addStaffToRegion(regionId, staffId)).thenReturn(dto);

        mvc.perform(post("/api/houses/regions/{rid}/staff/{sid}", regionId, staffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffIds[0]").value(staffId.toString()));
    }

    @Test
    @DisplayName("GET / returns all regions")
    void getAll() throws Exception {
        RegionDto dto = new RegionDto(UUID.randomUUID(), "N", "d", UUID.randomUUID(), List.of());
        when(regionService.getAllRegions()).thenReturn(List.of(dto));

        mvc.perform(get("/api/houses/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("N"));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when service throws NotFoundException")
    void getByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(regionService.getById(id))
                .thenThrow(new com.isums.houseservice.exceptions.NotFoundException("Region not found"));

        mvc.perform(get("/api/houses/regions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /{id} returns region when found")
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        RegionDto dto = new RegionDto(id, "N", "d", UUID.randomUUID(), List.of());
        when(regionService.getById(id)).thenReturn(dto);

        mvc.perform(get("/api/houses/regions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("DELETE /{regionId}/staff/{staffId} removes staff")
    void removeStaff() throws Exception {
        UUID regionId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        RegionDto dto = new RegionDto(regionId, "N", "d", UUID.randomUUID(), List.of());
        when(regionService.removeStaffFromRegion(regionId, staffId)).thenReturn(dto);

        mvc.perform(delete("/api/houses/regions/{rid}/staff/{sid}", regionId, staffId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /staff/{staffId} returns regions for staff")
    void getMyRegion() throws Exception {
        UUID staffId = UUID.randomUUID();
        RegionDto dto = new RegionDto(UUID.randomUUID(), "N", "d", UUID.randomUUID(), List.of(staffId));
        when(regionService.getRegionByStaffId(staffId)).thenReturn(List.of(dto));

        mvc.perform(get("/api/houses/regions/staff/{sid}", staffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].staffIds[0]").value(staffId.toString()));
    }
}
