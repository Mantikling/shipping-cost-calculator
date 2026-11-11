package com.example.shipping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.shipping.model.Role;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiKeyServiceTest {

    private final ApiKeyService service =
            new ApiKeyService(List.of("user-key"), List.of("admin-key"));

    @ParameterizedTest(name = "The one where an API key of [{0}] is rejected as missing")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("A missing or blank API key is rejected as missing")
    void missingOrBlankApiKeyIsRejectedAsMissing(String apiKey) {
        assertThatExceptionOfType(MissingApiKeyException.class)
                .isThrownBy(() -> service.authenticate(apiKey))
                .withMessage("Missing API key");
    }

    @ParameterizedTest(name = "The one where the known key [{0}] returns the {1} role")
    @CsvSource({
            "user-key,  USER",
            "admin-key, ADMIN",
    })
    @DisplayName("A known API key returns its configured role")
    void knownApiKeyReturnsItsConfiguredRole(String apiKey, Role expectedRole) {
        assertThat(service.authenticate(apiKey)).isEqualTo(expectedRole);
    }

    @ParameterizedTest(name = "The one where the API key [{0}] matches no configured key and is rejected as invalid")
    @ValueSource(strings = {"not-a-real-key", "USER-KEY"})
    @DisplayName("A key matching no configured key is rejected as invalid")
    void keyMatchingNoConfiguredKeyIsRejectedAsInvalid(String apiKey) {
        assertThatExceptionOfType(InvalidApiKeyException.class)
                .isThrownBy(() -> service.authenticate(apiKey))
                .withMessage("Invalid API key");
    }
}
