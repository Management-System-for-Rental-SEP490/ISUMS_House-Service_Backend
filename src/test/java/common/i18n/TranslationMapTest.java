package common.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationMapTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldUseAcceptLanguageHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ja,en;q=0.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwtWithLocale("en"), null, List.of()));

        TranslationMap translationMap = TranslationMap.of(Map.of(
                "vi", "Nhà",
                "en", "House",
                "ja", "ハウス"
        ));

        assertThat(translationMap.resolve()).isEqualTo("ハウス");
    }

    @Test
    void shouldFallbackToJwtLocaleWhenHeaderMissing() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwtWithLocale("en"), null, List.of()));

        TranslationMap translationMap = TranslationMap.of(Map.of(
                "vi", "Nhà",
                "en", "House",
                "ja", "ハウス"
        ));

        assertThat(translationMap.resolve()).isEqualTo("House");
    }

    private Jwt jwtWithLocale(String locale) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("locale", locale)
        );
    }
}
