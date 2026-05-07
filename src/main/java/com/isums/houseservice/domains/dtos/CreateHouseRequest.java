package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.emuns.HouseStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateHouseRequest(
    String name,
    String address,
    UUID regionId,
    String ward,
    String commune,
    String city,
    String description,
    Integer numberOfFloors,
    BigDecimal areaM2,
    HouseStructure structure,
    String landCertNumber,
    LocalDate landCertIssueDate,
    String landCertIssuer,
    List<String> houseImages,

    // Optional manager-supplied overrides per field. Keys are BCP-47 lang codes (en, ja, ...).
    // For any locale NOT in the override map, the auto-translate service will fill it.
    // The Vietnamese source stays in the top-level string fields above.
    Map<String, String> nameTranslations,
    Map<String, String> addressTranslations,
    Map<String, String> wardTranslations,
    Map<String, String> communeTranslations,
    Map<String, String> cityTranslations,
    Map<String, String> descriptionTranslations
) {
}
