package com.example.shipping.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@WithTestApiKeys
@DisplayName("Destination zone multipliers")
class DistanceZonesAcceptanceIT {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Must multiply the weight-tier base rate by the destination zone's multiplier")
    class MultiplyBaseRateByZoneMultiplier {

        @ParameterizedTest(name = "The one where a {0}kg {1} parcel with a base rate of £{2} is multiplied by ×{3} to give £{4}")
        @CsvSource({
                "3, DOMESTIC,      4.99, 1.0, 4.99",
                "3, EUROPEAN,      4.99, 1.5, 7.49",
                "3, INTERNATIONAL, 4.99, 2.5, 12.48",
        })
        void zonedRateIsBaseRateTimesZoneMultiplier(
                String weightKg, String zone, String expectedBaseRate, String expectedMultiplier, String expectedZonedRate) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "%s", "orderTotal": 50.00 }
                            """.formatted(weightKg, zone))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedBaseRate);
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zoneMultiplier")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedMultiplier);
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedZonedRate);
        }
    }

    @Nested
    @DisplayName("Must apply the zone multiplier after the base rate, including any over-20kg surcharge")
    class ZoneMultiplierAppliedAfterBaseRate {

        @Test
        @DisplayName("The one where a 25kg international parcel has a base rate of £11.49 (£8.99 + £2.50 surcharge), so it costs £11.49 × 2.5 = £28.73")
        void multiplierAppliesToBaseRateIncludingSurcharge() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 25, "zone": "INTERNATIONAL", "orderTotal": 50.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("11.49");
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("28.73");
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("28.73");
        }
    }

    @Nested
    @DisplayName("Must round the zoned rate to 2 decimal places, HALF_UP")
    class RoundZonedRateHalfUp {

        @Test
        @DisplayName("The one where a 0.5kg European parcel is £2.99 × 1.5 = £4.485, which rounds up to £4.49")
        void zonedRateIsRoundedHalfUpToTwoDecimalPlaces() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 0.5, "zone": "EUROPEAN", "orderTotal": 50.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.49");
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.49");
        }
    }

    @Nested
    @DisplayName("Must accept zone names in any letter case")
    class AcceptZoneNamesInAnyLetterCase {

        @ParameterizedTest(name = "The one where the zone \"{0}\" is accepted as {1} (×{2})")
        @CsvSource({
                "EUROPEAN,      European,      1.5",
                "european,      European,      1.5",
                "International, International, 2.5",
        })
        void zoneNamesAreMatchedIgnoringLetterCase(String zoneGiven, String acceptedAs, String expectedMultiplier) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "%s", "orderTotal": 50.00 }
                            """.formatted(zoneGiven))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zoneMultiplier")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedMultiplier);
        }
    }

    @Nested
    @DisplayName("Must reject a missing or unknown destination zone")
    class RejectMissingOrUnknownZone {

        @Test
        @DisplayName("The one where a parcel to zone \"MARS\" is rejected as invalid")
        void unknownZoneIsRejected() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "MARS", "orderTotal": 50.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where a parcel with no zone is rejected as invalid. It is not assumed to be domestic")
        void missingZoneIsRejected() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "orderTotal": 50.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where a 3kg parcel to \"DOMESTIC\" is accepted and charged £4.99")
        void knownZoneIsAccepted() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 50.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
        }
    }
}
