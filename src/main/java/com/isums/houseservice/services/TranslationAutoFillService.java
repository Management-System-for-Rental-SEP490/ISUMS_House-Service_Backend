package com.isums.houseservice.services;

import common.i18n.TranslationMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAutoFillService {

    private static final List<String> TARGET_LANGUAGES = List.of("en", "ja");

    private final TranslateClient translateClient;

    /**
     * Auto-translate Vietnamese text into all target locales.
     * Convenience wrapper around {@link #complete(String, Map)} with no manual overrides.
     */
    public TranslationMap complete(String vietnameseText) {
        return complete(vietnameseText, Map.of());
    }

    /**
     * Auto-translate Vietnamese text into all target locales, honouring
     * manager-supplied overrides. For any locale present (with non-blank value)
     * in {@code overrides}, the manager's text is kept verbatim and AWS Translate
     * is NOT called for that locale. Missing / blank locales fall back to AI translation.
     *
     * @param vietnameseText the Vietnamese source; returns null if blank
     * @param overrides      manager-supplied map of locale to text; may be null / empty
     */
    public TranslationMap complete(String vietnameseText, Map<String, String> overrides) {
        if (vietnameseText == null || vietnameseText.isBlank()) {
            return null;
        }

        String source = vietnameseText.trim();
        Map<String, String> translations = new LinkedHashMap<>();
        translations.put("vi", source);

        Map<String, String> normalized = normalizeOverrides(overrides);
        // Allow manager to also override vi text (rare, but explicit)
        String viOverride = normalized.get("vi");
        if (viOverride != null) {
            translations.put("vi", viOverride);
        }

        for (String target : TARGET_LANGUAGES) {
            String manual = normalized.get(target);
            if (manual != null) {
                translations.put(target, manual);
            } else {
                translations.put(target, translateOrFallback(source, target));
            }
        }

        return TranslationMap.of(translations);
    }

    private Map<String, String> normalizeOverrides(Map<String, String> overrides) {
        Map<String, String> out = new LinkedHashMap<>();
        if (overrides == null || overrides.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            out.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().trim());
        }
        return out;
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
