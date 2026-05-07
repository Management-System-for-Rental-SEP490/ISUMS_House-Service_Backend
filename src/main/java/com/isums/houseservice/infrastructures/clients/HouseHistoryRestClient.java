package com.isums.houseservice.infrastructures.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.dtos.ApiResponse;
import common.paginations.dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HouseHistoryRestClient {
    private static final int PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;

    @Value("${maintenance.service.url:http://localhost:8086}")
    private String maintenanceServiceUrl;

    @Value("${issue.service.url:http://localhost:8092}")
    private String issueServiceUrl;

    public List<MaintenanceJobItemDto> getMaintenanceJobsByHouseId(UUID houseId, String bearerToken) {
        try {
            HttpRequest request = baseRequest(maintenanceServiceUrl + "/api/maintenances/jobs/house/" + houseId, bearerToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Maintenance jobs endpoint returned HTTP " + response.statusCode());
            }

            ApiResponse<List<MaintenanceJobItemDto>> payload = objectMapper.readValue(
                    response.body(),
                    new TypeReference<>() {}
            );
            return payload == null || payload.getData() == null ? List.of() : payload.getData();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load maintenance jobs by house: " + ex.getMessage(), ex);
        }
    }

    public List<InspectionItemDto> getInspectionsByHouseId(UUID houseId, String bearerToken) {
        try {
            return loadPagedItems(
                    maintenanceServiceUrl + "/api/maintenances/inspections",
                    "houseId=" + encode(houseId.toString()) + "&size=" + PAGE_SIZE + "&sorts=createdAt:desc",
                    bearerToken,
                    new TypeReference<ApiResponse<PageResponse<InspectionItemDto>>>() {}
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load inspections by house: " + ex.getMessage(), ex);
        }
    }

    public List<IssueTicketItemDto> getIssueTicketsByHouseId(UUID houseId, String bearerToken) {
        try {
            return loadPagedItems(
                    issueServiceUrl + "/api/issues/tickets",
                    "houseId=" + encode(houseId.toString()) + "&size=" + PAGE_SIZE + "&sorts=createdAt:desc",
                    bearerToken,
                    new TypeReference<ApiResponse<PageResponse<IssueTicketItemDto>>>() {}
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load issue tickets by house: " + ex.getMessage(), ex);
        }
    }

    private <T> List<T> loadPagedItems(
            String baseUrl,
            String fixedQuery,
            String bearerToken,
            TypeReference<ApiResponse<PageResponse<T>>> typeReference
    ) throws Exception {
        int page = 1;
        boolean hasMore = true;
        java.util.ArrayList<T> items = new java.util.ArrayList<>();

        while (hasMore && page <= 20) {
            String query = fixedQuery + "&page=" + page;
            HttpRequest request = baseRequest(baseUrl + "?" + query, bearerToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Paged endpoint returned HTTP " + response.statusCode());
            }

            ApiResponse<PageResponse<T>> payload = objectMapper.readValue(response.body(), typeReference);
            if (payload == null || payload.getData() == null || payload.getData().items() == null) {
                break;
            }

            items.addAll(payload.getData().items());
            hasMore = payload.getData().hasMore();
            page++;
        }

        return items;
    }

    private HttpRequest.Builder baseRequest(String url, String bearerToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header(HttpHeaders.ACCEPT, "application/json");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record MaintenanceJobItemDto(
            UUID id,
            UUID planId,
            UUID houseId,
            UUID assignedStaffId,
            String staffName,
            String staffPhone,
            Object staff,
            LocalDate periodStartDate,
            String status
    ) {
    }

    public record InspectionItemDto(
            UUID id,
            UUID houseId,
            UUID eContractId,
            UUID assignedStaffId,
            String staffName,
            String staffPhone,
            UUID slotId,
            String status,
            String type,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record IssueTicketItemDto(
            UUID id,
            UUID tenantId,
            String tenantPhone,
            UUID houseId,
            UUID assetId,
            UUID assignedStaffId,
            String staffName,
            String staffPhone,
            UUID slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String type,
            String status,
            String title,
            String description,
            Instant createdAt,
            List<Object> images,
            Object tenant,
            Object assignedStaff,
            Object house,
            Object asset
    ) {
    }
}
