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
@DisplayName("Weight-based shipping rates")
class WeightTiersAcceptanceIT {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Must charge a flat base rate for the weight tier the parcel falls in, with each tier's lower bound inclusive")
    class FlatBaseRatePerWeightTier {

        @ParameterizedTest(name = "The one where a {0}kg parcel has a base rate of £{1}")
        @CsvSource({
                "0.5,  2.99",
                "0.99, 2.99",
                "1,    4.99",
                "3,    4.99",
                "5,    8.99",
                "10,   8.99",
                "20,   8.99",
        })
        void baseRateIsFlatPerWeightTierWithLowerBoundInclusive(String weightKg, String expectedBaseRate) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "DOMESTIC", "orderTotal": 50.00 }
                            """.formatted(weightKg))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedBaseRate);
        }
    }

    @Nested
    @DisplayName("Must add £0.50 per kg over 20kg to the £8.99 base rate, pro-rata for fractions")
    class SurchargePerKgOverTwenty {

        @ParameterizedTest(name = "The one where a {0}kg parcel has a base rate of £{1}")
        @CsvSource({
                "25,    11.49",
                "20.5,  9.24",
                "20.01, 9.00",
        })
        void baseRateAddsFiftyPencePerKgOverTwenty(String weightKg, String expectedBaseRate) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "DOMESTIC", "orderTotal": 50.00 }
                            """.formatted(weightKg))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedBaseRate);
        }
    }

    @Nested
    @DisplayName("Must reject a parcel weight of zero or less")
    class RejectWeightOfZeroOrLess {

        @ParameterizedTest(name = "The one where a {0}kg parcel is rejected as invalid")
        @CsvSource({
                "0",
                "-2",
        })
        void weightOfZeroOrLessIsRejected(String weightKg) {
            MvcTestResult result = calculate(weightKg);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where a 0.01kg parcel is accepted and charged £2.99")
        void weightOfOneHundredthOfAKgIsAccepted() {
            MvcTestResult result = calculate("0.01");

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("2.99");
        }

        private MvcTestResult calculate(String weightKg) {
            return mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "DOMESTIC", "orderTotal": 50.00 }
                            """.formatted(weightKg))
                    .exchange();
        }
    }

    @Nested
    @DisplayName("Must reject a parcel weight over 50kg")
    class RejectWeightOverFiftyKg {

        @Test
        @DisplayName("The one where a 50.01kg parcel is rejected as too heavy")
        void weightOverFiftyKgIsRejected() {
            MvcTestResult result = calculate("50.01");

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where a 50kg parcel is accepted and charged £8.99 + 30 × £0.50 = £23.99")
        void fiftyKgParcelIsAccepted() {
            MvcTestResult result = calculate("50");

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("23.99");
        }

        private MvcTestResult calculate(String weightKg) {
            return mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "DOMESTIC", "orderTotal": 50.00 }
                            """.formatted(weightKg))
                    .exchange();
        }
    }
}
