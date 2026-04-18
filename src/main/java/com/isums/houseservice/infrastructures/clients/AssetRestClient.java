package com.isums.houseservice.infrastructures.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetRestClient {
    private final ObjectMapper objectMapper;

    @Value("${asset.service.url:http://localhost:18090}")
    private String assetServiceUrl;

    public List<AreaAssetCountDto> getAssetCountByHouseId(UUID houseId, String bearerToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(assetServiceUrl + "/api/assets/items/house/" + houseId + "/function-area-counts"))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Asset count endpoint returned HTTP " + response.statusCode());
            }

            ApiResponse<List<AreaAssetCountDto>> payload = objectMapper.readValue(
                    response.body(),
                    new TypeReference<>() {}
            );

            if (payload == null || payload.getData() == null) {
                return List.of();
            }
            return payload.getData();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load asset count by house: " + ex.getMessage(), ex);
        }
    }

    public record AreaAssetCountDto(
            UUID functionAreaId,
            long assetCount
    ) {
    }
}
