package com.isums.houseservice.services;

import common.i18n.TranslationMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationAutoFillService")
class TranslationAutoFillServiceTest {

    @Mock private TranslateClient translateClient;

    @InjectMocks
    private TranslationAutoFillService service;

    private TranslateTextResponse resp(String text) {
        return TranslateTextResponse.builder().translatedText(text).build();
    }

    @Test
    @DisplayName("blank / null vi input returns null, no AWS call")
    void blankInput_returnsNull() {
        assertThat(service.complete(null)).isNull();
        assertThat(service.complete("")).isNull();
        assertThat(service.complete("   ")).isNull();
        verifyNoInteractions(translateClient);
    }

    @Test
    @DisplayName("no overrides: AWS called for en + ja, result has all 3 locales")
    void noOverrides_callsAwsForBothTargets() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode()))))
                .willReturn(resp("Hieu House D7"));
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "ja".equals(r.targetLanguageCode()))))
                .willReturn(resp("ヒュー邸 D7"));

        TranslationMap result = service.complete("Nhà Hiệu Q7");

        assertThat(result).isNotNull();
        Map<String, String> tr = result.getTranslations();
        assertThat(tr).containsEntry("vi", "Nhà Hiệu Q7");
        assertThat(tr).containsEntry("en", "Hieu House D7");
        assertThat(tr).containsEntry("ja", "ヒュー邸 D7");

        verify(translateClient, times(2)).translateText(any(TranslateTextRequest.class));
    }

    @Test
    @DisplayName("EN manual override: AWS called for ja ONLY, en is the manager's text")
    void withEnOverride_awsCalledForJaOnly() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "ja".equals(r.targetLanguageCode()))))
                .willReturn(resp("ヒュー邸 D7"));

        TranslationMap result = service.complete("Nhà Hiệu Q7",
                Map.of("en", "Hieu Mansion District 7"));

        assertThat(result.getTranslations())
                .containsEntry("vi", "Nhà Hiệu Q7")
                .containsEntry("en", "Hieu Mansion District 7")
                .containsEntry("ja", "ヒュー邸 D7");

        // Should NEVER ask AWS for en
        verify(translateClient, never()).translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode())));
        verify(translateClient, times(1)).translateText(any(TranslateTextRequest.class));
    }

    @Test
    @DisplayName("EN + JA overrides: AWS NEVER called at all")
    void withFullOverrides_awsNotCalled() {
        TranslationMap result = service.complete("Nhà Hiệu Q7",
                Map.of("en", "Hieu Mansion D7", "ja", "ヒュー邸 D7 (手動)"));

        assertThat(result.getTranslations())
                .containsEntry("vi", "Nhà Hiệu Q7")
                .containsEntry("en", "Hieu Mansion D7")
                .containsEntry("ja", "ヒュー邸 D7 (手動)");
        verifyNoInteractions(translateClient);
    }

    @Test
    @DisplayName("blank / null override values are ignored → AWS fills in those locales")
    void blankOverrides_fallbackToAws() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode()))))
                .willReturn(resp("Hieu House D7"));
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "ja".equals(r.targetLanguageCode()))))
                .willReturn(resp("ヒュー邸 D7"));

        Map<String, String> overrides = new HashMap<>();
        overrides.put("en", "   ");   // blank → ignored
        overrides.put("ja", null);    // null → ignored

        TranslationMap result = service.complete("Nhà Hiệu Q7", overrides);

        assertThat(result.getTranslations())
                .containsEntry("en", "Hieu House D7")
                .containsEntry("ja", "ヒュー邸 D7");
        verify(translateClient, times(2)).translateText(any(TranslateTextRequest.class));
    }

    @Test
    @DisplayName("override locale keys are normalized (trim + lowercase)")
    void overrideKeys_normalized() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode()))))
                .willReturn(resp("Hieu House D7"));

        TranslationMap result = service.complete("Nhà Hiệu Q7",
                Map.of("  JA  ", "大文字から正規化"));

        assertThat(result.getTranslations())
                .containsEntry("ja", "大文字から正規化")
                .containsEntry("en", "Hieu House D7");
    }

    @Test
    @DisplayName("manager may override vi itself (rare but explicit)")
    void viOverride_replacesSource() {
        given(translateClient.translateText(any(TranslateTextRequest.class)))
                .willAnswer(inv -> {
                    TranslateTextRequest r = inv.getArgument(0);
                    return resp("AI[" + r.targetLanguageCode() + "]");
                });

        TranslationMap result = service.complete("Nhà Hiệu Q7",
                Map.of("vi", "Nhà Hiệu Q7 đã chỉnh"));

        assertThat(result.getTranslations())
                .containsEntry("vi", "Nhà Hiệu Q7 đã chỉnh")
                .containsEntry("en", "AI[en]")
                .containsEntry("ja", "AI[ja]");
    }

    @Test
    @DisplayName("when AWS throws, that locale falls back to the Vietnamese source")
    void awsThrows_fallbackToSource() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode()))))
                .willThrow(new RuntimeException("quota exceeded"));
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "ja".equals(r.targetLanguageCode()))))
                .willReturn(resp("ヒュー邸"));

        TranslationMap result = service.complete("Nhà Hiệu");

        assertThat(result.getTranslations())
                .containsEntry("vi", "Nhà Hiệu")
                .containsEntry("en", "Nhà Hiệu")  // fallback to source
                .containsEntry("ja", "ヒュー邸");
    }

    @Test
    @DisplayName("1-arg overload delegates to 2-arg with empty overrides")
    void oneArgOverload_equalsTwoArgWithEmpty() {
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "en".equals(r.targetLanguageCode()))))
                .willReturn(resp("EN"));
        given(translateClient.translateText(argThat((TranslateTextRequest r) ->
                r != null && "ja".equals(r.targetLanguageCode()))))
                .willReturn(resp("JA"));

        TranslationMap a = service.complete("src");
        TranslationMap b = service.complete("src", Map.of());
        TranslationMap c = service.complete("src", null);

        assertThat(a.getTranslations()).isEqualTo(b.getTranslations());
        assertThat(a.getTranslations()).isEqualTo(c.getTranslations());
    }
}
