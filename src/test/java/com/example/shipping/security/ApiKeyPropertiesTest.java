package com.example.shipping.security;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiKeyPropertiesTest {

    @Test
    @DisplayName("The one where a configured key list is accepted")
    void configuredKeysAreAccepted() {
        new ApiKeyProperties(List.of("user-key"), List.of("admin-key"));
    }

    @ParameterizedTest(name = "The one where the USER key list is {0} and the application refuses to start")
    @MethodSource("unusableUserKeyLists")
    @DisplayName("An unset or unresolved USER key list stops the application starting")
    void unusableUserKeysStopStartup(List<String> userKeys) {
        assertThatIllegalStateException()
                .isThrownBy(() -> new ApiKeyProperties(userKeys, List.of("admin-key")))
                .withMessageContaining("api.keys.user");
    }

    static Stream<List<String>> unusableUserKeyLists() {
        return Stream.of(
                null,
                List.of(),
                List.of("${API_USER_KEYS}"),
                List.of(" "));
    }
}
