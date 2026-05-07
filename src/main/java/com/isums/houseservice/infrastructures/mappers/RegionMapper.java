package com.isums.houseservice.infrastructures.mappers;

import common.i18n.TranslationMap;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.entities.Region;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RegionMapper {

    @Mapping(target = "staffIds", source = "staffIds")
    @Mapping(target = "name", expression = "java(resolveLocalized(region.getName(), region.getNameTranslations()))")
    @Mapping(target = "description", expression = "java(resolveLocalized(region.getDescription(), region.getDescriptionTranslations()))")
    RegionDto toDto(Region region, List<UUID> staffIds);

    List<RegionDto> toDtos(List<Region> regions);

    default String resolveLocalized(String source, TranslationMap translations) {
        if (translations == null || translations.getTranslations().isEmpty()) {
            return source;
        }
        String resolved = translations.resolve();
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }

}
