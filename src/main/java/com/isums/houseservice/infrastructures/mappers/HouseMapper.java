package com.isums.houseservice.infrastructures.mappers;

import common.i18n.TranslationMap;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                FunctionalAreaMapper.class,
                HouseImageMapper.class
        }
)
public interface HouseMapper {
    @Mapping(source = "region.id", target = "regionId")
    @Mapping(target = "functionalAreas", source = "functionalAreas")
    @Mapping(target = "images", source = "houseImages")
    @Mapping(target = "name", expression = "java(resolveLocalized(house.getName(), house.getNameTranslations()))")
    @Mapping(target = "description", expression = "java(resolveLocalized(house.getDescription(), house.getDescriptionTranslations()))")
    HouseDto toDto(House house);
    List<HouseDto> toDtos(List<House> houses);

    default String resolveLocalized(String source, TranslationMap translations) {
        if (translations == null || translations.getTranslations().isEmpty()) {
            return source;
        }
        String resolved = translations.resolve();
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }
}
