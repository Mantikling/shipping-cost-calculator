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
@DisplayName("Free shipping for qualifying domestic orders")
class FreeShippingAcceptanceIT {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Must make shipping free for a domestic order whose order total is £75.00 or more")
    class FreeShippingForDomesticOrdersOver75 {

        @ParameterizedTest(name = "The one where a {0}kg domestic parcel on a £{1} order has zoned rate £{2}, free shipping applied {3}, and total cost £{4}")
        @CsvSource({
                "3,  100.00, 4.99,  true,  0.00",
                "25, 80.00,  11.49, true,  0.00",
                "3,  75.00,  4.99,  true,  0.00",
                "3,  74.99,  4.99,  false, 4.99",
        })
        void domesticOrderAtOrAbove75GetsFreeShipping(
                String weightKg, String orderTotal, String expectedZonedRate,
                boolean expectedFreeShippingApplied, String expectedTotalCost) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "DOMESTIC", "orderTotal": %s }
                            """.formatted(weightKg, orderTotal))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedZonedRate);
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.freeShippingApplied")
                    .asBoolean()
                    .isEqualTo(expectedFreeShippingApplied);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedTotalCost);
        }
    }

    @Nested
    @DisplayName("Must never make shipping free for European or international orders, whatever the order total")
    class NoFreeShippingOutsideDomestic {

        @ParameterizedTest(name = "The one where a {0}kg {1} parcel on a £{2} order has free shipping applied {3} and costs £{4}")
        @CsvSource({
                "3, EUROPEAN,      500.00, false, 7.49",
                "3, INTERNATIONAL, 500.00, false, 12.48",
                "3, DOMESTIC,      500.00, true,  0.00",
        })
        void europeanAndInternationalOrdersNeverGetFreeShipping(
                String weightKg, String zone, String orderTotal,
                boolean expectedFreeShippingApplied, String expectedTotalCost) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "%s", "orderTotal": %s }
                            """.formatted(weightKg, zone, orderTotal))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.freeShippingApplied")
                    .asBoolean()
                    .isEqualTo(expectedFreeShippingApplied);
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo(expectedTotalCost);
        }
    }

    @Nested
    @DisplayName("Must still show the waived charge in the breakdown when shipping is free")
    class BreakdownShowsWaivedCharge {

        @Test
        @DisplayName("The one where a 3kg domestic parcel on a £100.00 order shows base rate £4.99, multiplier ×1.0, zoned rate £4.99, free shipping applied, and total cost £0.00")
        void freeShippingStillShowsEveryStepOfTheBreakdown() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 100.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.baseRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zoneMultiplier")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("1.0");
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.freeShippingApplied")
                    .asBoolean()
                    .isTrue();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("The one where a 3kg domestic parcel on a £74.99 order shows free shipping not applied and total cost £4.99")
        void belowThresholdShowsFreeShippingNotApplied() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 74.99 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.freeShippingApplied")
                    .asBoolean()
                    .isFalse();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
        }
    }

    @Nested
    @DisplayName("Must reject a missing or negative order total")
    class RejectMissingOrNegativeOrderTotal {

        @ParameterizedTest(name = "The one where a {0} request with no order total is rejected as invalid")
        @CsvSource({
                "DOMESTIC",
                "EUROPEAN",
                "INTERNATIONAL",
        })
        void missingOrderTotalIsRejectedForAnyZone(String zone) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "%s" }
                            """.formatted(zone))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where an order total of −£10.00 is rejected as invalid")
        void negativeOrderTotalIsRejected() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": -10.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("The one where a 3kg domestic parcel on a £0.00 order is accepted and costs £4.99")
        void zeroOrderTotalIsAccepted() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 0.00 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
        }
    }

    @Nested
    @DisplayName("Must compare the order total exactly as given, without rounding it")
    class CompareOrderTotalWithoutRounding {

        @Test
        @DisplayName("The one where a domestic order total of £74.995 is under £75.00, so it does not qualify and costs £4.99")
        void orderTotalJustUnderThresholdIsNotRoundedUp() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 74.995 }
                            """)
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.freeShippingApplied")
                    .asBoolean()
                    .isFalse();
            assertThat(result).bodyJson()
                    .extractingPath("$.totalCost")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("4.99");
        }
    }
}
