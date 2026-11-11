package com.example.shipping.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "api.keys.user=test-user-key",
        "api.keys.admin=test-admin-key"
})
@DisplayName("API security with API key authentication")
class ApiSecurityAcceptanceIT {

    private static final String CALCULATE_3KG_DOMESTIC = """
            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 10.00 }
            """;

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Must reject a request to a protected endpoint that carries no API key")
    class RejectRequestWithNoApiKey {

        @Test
        @DisplayName("The one where a shipping calculation with no X-API-Key header is rejected as unauthenticated (401, \"Missing API key\")")
        void calculationWithNoApiKeyHeaderIsRejected() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CALCULATE_3KG_DOMESTIC)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("Unauthorized");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Missing API key");
        }

        @Test
        @DisplayName("The one where a shipping calculation with an empty X-API-Key header is rejected the same way (401, \"Missing API key\")")
        void calculationWithEmptyApiKeyHeaderIsRejected() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CALCULATE_3KG_DOMESTIC)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("Unauthorized");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Missing API key");
        }

        @Test
        @DisplayName("The one where the same calculation with a valid USER key is accepted and a 3kg domestic parcel on a £10.00 order costs £4.99")
        void calculationWithValidUserKeyIsAccepted() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", "test-user-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CALCULATE_3KG_DOMESTIC)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
        }
    }

    @Nested
    @DisplayName("Must reject a request whose API key doesn't exactly match a configured key")
    class RejectRequestWithUnmatchedApiKey {

        // "not-a-real-key" is the spec's example; "TEST-USER-KEY" is its counter-example:
        // the configured USER key in a different letter case (keys match exactly).
        @ParameterizedTest(name = "The one where a calculation with the key [{0}] is rejected as unauthenticated (401, \"Invalid API key\")")
        @ValueSource(strings = {"not-a-real-key", "TEST-USER-KEY"})
        void calculationWithUnmatchedApiKeyIsRejected(String apiKey) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CALCULATE_3KG_DOMESTIC)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("Unauthorized");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Invalid API key");
        }
    }

    @Nested
    @DisplayName("Must keep the API documentation open to everyone, whatever key is sent")
    class KeepApiDocumentationOpen {

        @Test
        @DisplayName("The one where Swagger UI opens with no key")
        void swaggerUiOpensWithNoApiKey() {
            MvcTestResult result = mvc.get().uri("/swagger-ui.html").exchange();

            assertThat(result).hasStatus3xxRedirection();
            assertThat(result).headers().hasValue("Location", "/swagger-ui/index.html");
        }
    }

    @Nested
    @DisplayName("Must grant access to each endpoint based on the key's role")
    class GrantAccessByRole {

        @Test
        @DisplayName("The one where a USER key viewing admin rates is rejected as not authorised (403, \"Insufficient role\")")
        void userKeyCannotViewAdminRates() {
            MvcTestResult result = mvc.get().uri("/api/admin/rates")
                    .header("X-API-Key", TestApiKeys.USER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("Forbidden");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Insufficient role");
        }

        @Test
        @DisplayName("The one where an ADMIN key viewing admin rates is accepted")
        void adminKeyCanViewAdminRates() {
            MvcTestResult result = mvc.get().uri("/api/admin/rates")
                    .header("X-API-Key", TestApiKeys.ADMIN)
                    .exchange();

            assertThat(result).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Must return the current rate card to an ADMIN viewing admin rates")
    class ReturnRateCardToAdmin {

        @Test
        @DisplayName("The one where an ADMIN sees weight tiers £2.99 (from 0kg), £4.99 (from 1kg) and £8.99 (from 5kg), a £0.50 per kg surcharge over 20kg, zone multipliers domestic ×1.0, European ×1.5 and international ×2.5, and a domestic free-shipping threshold of £75.00")
        void adminSeesTheCurrentRateCard() {
            MvcTestResult result = mvc.get().uri("/api/admin/rates")
                    .header("X-API-Key", TestApiKeys.ADMIN)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
            assertMoney(result, "$.weightTiers[0].fromKg", "0");
            assertMoney(result, "$.weightTiers[0].rate", "2.99");
            assertMoney(result, "$.weightTiers[1].fromKg", "1");
            assertMoney(result, "$.weightTiers[1].rate", "4.99");
            assertMoney(result, "$.weightTiers[2].fromKg", "5");
            assertMoney(result, "$.weightTiers[2].rate", "8.99");
            assertMoney(result, "$.overweightSurcharge.overKg", "20");
            assertMoney(result, "$.overweightSurcharge.perKg", "0.50");
            assertMoney(result, "$.zoneMultipliers.DOMESTIC", "1.0");
            assertMoney(result, "$.zoneMultipliers.EUROPEAN", "1.5");
            assertMoney(result, "$.zoneMultipliers.INTERNATIONAL", "2.5");
            assertThat(result).bodyJson().extractingPath("$.freeShipping.zone").isEqualTo("DOMESTIC");
            assertMoney(result, "$.freeShipping.minimumOrderTotal", "75.00");
        }

        private void assertMoney(MvcTestResult result, String jsonPath, String expected) {
            assertThat(result).bodyJson()
                    .extractingPath(jsonPath)
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expected);
        }
    }
}
