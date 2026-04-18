package common.i18n;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class TranslationMap {
    private final Map<String, String> translations;

    public TranslationMap() {
        this.translations = new LinkedHashMap<>();
    }

    public TranslationMap(Map<String, String> translations) {
        this.translations = normalize(translations);
    }

    public static TranslationMap of(Map<String, String> translations) {
        return new TranslationMap(translations);
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public String resolve() {
        if (translations.isEmpty()) return null;
        String lang = LocaleContextHolder.getLocale().getLanguage();
        if (lang == null || lang.isBlank()) lang = Locale.getDefault().getLanguage();
        String direct = translations.get(lang);
        if (direct != null && !direct.isBlank()) return direct;
        String vi = translations.get("vi");
        if (vi != null && !vi.isBlank()) return vi;
        String en = translations.get("en");
        if (en != null && !en.isBlank()) return en;
        return translations.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static Map<String, String> normalize(Map<String, String> input) {
        Map<String, String> out = new LinkedHashMap<>();
        if (input == null) return out;
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            out.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().trim());
        }
        return out;
    }
}
