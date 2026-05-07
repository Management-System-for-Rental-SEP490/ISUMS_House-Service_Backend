package com.isums.houseservice.domains.dtos;

import java.util.Map;

public record UpdateHouseTranslationsRequest(
        Map<String, String> nameTranslations,
        Map<String, String> addressTranslations,
        Map<String, String> wardTranslations,
        Map<String, String> communeTranslations,
        Map<String, String> cityTranslations,
        Map<String, String> descriptionTranslations
) {
}
