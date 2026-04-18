package com.isums.houseservice.services;

import common.i18n.TranslationMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAutoFillService {

    private static final List<String> TARGET_LANGUAGES = List.of("en", "ja");

    private final TranslateClient translateClient;

    public TranslationMap complete(String vietnameseText) {
        if (vietnameseText == null || vietnameseText.isBlank()) {
            return null;
        }

        String source = vietnameseText.trim();
        Map<String, String> translations = new LinkedHashMap<>();
        translations.put("vi", source);

        for (String target : TARGET_LANGUAGES) {
            translations.put(target, translateOrFallback(source, target));
        }

        return TranslationMap.of(translations);
    }

    private String translateOrFallback(String source, String targetLanguage) {
        try {
            return translateClient.translateText(TranslateTextRequest.builder()
                    .text(source)
                    .sourceLanguageCode("vi")
                    .targetLanguageCode(targetLanguage)
                    .build()).translatedText();
        } catch (Exception ex) {
            log.warn("House translation fallback target={}: {}", targetLanguage, ex.getMessage());
            return source;
        }
    }
}
