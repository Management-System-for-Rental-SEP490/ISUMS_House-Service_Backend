package com.isums.houseservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.dtos.HouseAccessStatus;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.emuns.AccessStatus;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.exceptions.GlobalExceptionHandler;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import common.paginations.dtos.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("HouseController")
class HouseControllerTest {

    @Mock private HouseService houseService;
    @Mock private UserClientsGrpc userClientsGrpc;

    @InjectMocks private HouseController controller;

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
                                                    NativeWebRequest w, WebDataBinderFactory b) { return jwt; }
        };

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HouseDto houseDto(UUID id) {
        return new HouseDto(id, UUID.randomUUID(), UUID.randomUUID(), "H1",
                "addr", "ward", "commune", "city", 1,
                null, null, null, null, null,   // areaM2, structure, landCert_*
                false, "desc",
                HouseStatus.AVAILABLE, List.of(), List.of(),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of(),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
    }

    @Test
    @DisplayName("GET / returns paginated list")
    void getAll() throws Exception {
        UUID id = UUID.randomUUID();
        PageResponse<HouseDto> page = PageResponse.of(
                List.of(houseDto(id)), false, 1L, 1, 0, 10);
        when(houseService.getAll(any())).thenReturn(page);

        mvc.perform(get("/api/houses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("H1"));
    }

    @Test
    @DisplayName("GET / returns 400 when page is less than 1")
    void getAll_invalidPage_returnsBadRequest() throws Exception {
        mvc.perform(get("/api/houses").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"));

        verifyNoInteractions(houseService);
    }

    @Test
    @DisplayName("POST / creates house and returns 200 (body statusCode=201)")
    void create() throws Exception {
        CreateHouseRequest req = new CreateHouseRequest(
                "H1","addr",UUID.randomUUID(),"ward","commune","city","desc", 1,
                null, null, null, null, null,   // areaM2, structure, landCert_*
                List.of(),
                null, null, null, null, null, null);
        UUID id = UUID.randomUUID();
        when(houseService.CreateHouse(any())).thenReturn(houseDto(id));

        mvc.perform(post("/api/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when service throws NotFound")
    void getByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(houseService.getHouseById(id)).thenThrow(new NotFoundException("no house"));

        mvc.perform(get("/api/houses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /{id} returns house when found")
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(houseService.getHouseById(id)).thenReturn(houseDto(id));

        mvc.perform(get("/api/houses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /house uses jwt subject and returns user houses")
    void getMyHouses() throws Exception {
        when(houseService.getHouseByUserId(keycloakId)).thenReturn(List.of(houseDto(UUID.randomUUID())));

        mvc.perform(get("/api/houses/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("H1"));

        verify(houseService).getHouseByUserId(keycloakId);
    }

    @Test
    @DisplayName("POST /{houseId}/images uploads files and returns 200")
    void uploadImages() throws Exception {
        UUID houseId = UUID.randomUUID();
        MockMultipartFile file1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{2});

        mvc.perform(multipart("/api/houses/{houseId}/images", houseId).file(file1).file(file2))
                .andExpect(status().isOk());

        verify(houseService).uploadHouseImages(eq(houseId), anyList());
    }

    @Test
    @DisplayName("DELETE /{houseId}/image/{imageId} deletes and returns 200")
    void deleteImage() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        mvc.perform(delete("/api/houses/{houseId}/image/{imageId}", houseId, imageId))
                .andExpect(status().isOk());

        verify(houseService).deleteHouseImage(houseId, imageId);
    }

    @Test
    @DisplayName("GET /my-access returns access statuses")
    void myAccess() throws Exception {
        HouseAccessStatus s = new HouseAccessStatus(UUID.randomUUID(), "H1", "addr",
                Instant.now(), AccessStatus.ACCESSIBLE, null, false, null, HouseMemberRole.OWNER);
        when(houseService.getMyHouseAccess(keycloakId)).thenReturn(List.of(s));

        mvc.perform(get("/api/houses/my-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accessStatus").value("ACCESSIBLE"))
                .andExpect(jsonPath("$.data[0].memberRole").value("OWNER"));
    }

    @Test
    @DisplayName("GET /region/{regionId} returns houses in region")
    void byRegion() throws Exception {
        UUID regionId = UUID.randomUUID();
        when(houseService.getHousesByRegionId(regionId)).thenReturn(List.of(houseDto(UUID.randomUUID())));

        mvc.perform(get("/api/houses/region/{rid}", regionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("H1"));
    }
}
