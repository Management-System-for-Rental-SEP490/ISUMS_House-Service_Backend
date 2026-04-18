package common.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter(autoApply = false)
public class TranslationMapConverter implements AttributeConverter<TranslationMap, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(TranslationMap attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute.getTranslations());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize translations", e);
        }
    }

    @Override
    public TranslationMap convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new TranslationMap();
        try {
            return TranslationMap.of(MAPPER.readValue(dbData, MAP_TYPE));
        } catch (Exception ignored) {
            return TranslationMap.of(Map.of("vi", dbData));
        }
    }
}
