package com.isums.houseservice.domains.mapper;


import common.i18n.TranslationMap;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.entities.FunctionalArea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FunctionalAreaMapper {
    @Mapping(source = "house.id",target = "houseId")
    @Mapping(target = "name", expression = "java(resolveLocalized(functionalArea.getName(), functionalArea.getNameTranslations()))")
    @Mapping(target = "description", expression = "java(resolveLocalized(functionalArea.getDescription(), functionalArea.getDescriptionTranslations()))")
    FunctionalAreaDto mapFunc (FunctionalArea functionalArea);
    List<FunctionalAreaDto> mapFuncs (List<FunctionalArea> functionalAreas);

    default String resolveLocalized(String source, TranslationMap translations) {
        if (translations == null || translations.getTranslations().isEmpty()) {
            return source;
        }
        String resolved = translations.resolve();
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }
}
