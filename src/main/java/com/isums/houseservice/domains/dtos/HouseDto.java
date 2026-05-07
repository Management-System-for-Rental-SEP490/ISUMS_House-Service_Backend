package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.emuns.HouseStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record HouseDto(UUID id,
                       UUID userRentalId,
                       UUID regionId,
                       String name,
                       String address,
                       String ward,
                       String commune,
                       String city,
                       Integer numberOfFloors,
                       BigDecimal areaM2,
                       HouseStructure structure,
                       String landCertNumber,
                       LocalDate landCertIssueDate,
                       String landCertIssuer,
                       Boolean paymentRestricted,
                       String description,
                       HouseStatus status,
                       List<FunctionalAreaDto> functionalAreas,
                       List<HouseImageDto> images,
                       Map<String, String> nameTranslations,
                       Map<String, String> addressTranslations,
                       Map<String, String> wardTranslations,
                       Map<String, String> communeTranslations,
                       Map<String, String> cityTranslations,
                       Map<String, String> descriptionTranslations)

{
}