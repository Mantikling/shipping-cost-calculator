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
@DisplayName("Free shipping for qualifying domestic and European orders")
class FreeShippingAcceptanceIT {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Must make shipping free for a domestic or European order whose order total is £50.00 or more")
    class FreeShippingForDomesticAndEuropeanOrdersOver50 {

        @ParameterizedTest(name = "The one where a {1}kg {0} parcel on a £{2} order has zoned rate £{3}, free shipping applied {4}, and total cost £{5}")
        @CsvSource({
                "DOMESTIC,  3,  100.00, 4.99,  true,  0.00",
                "DOMESTIC,  25, 80.00,  11.49, true,  0.00",
                "DOMESTIC,  3,  50.00,  4.99,  true,  0.00",
                "DOMESTIC,  3,  49.99,  4.99,  false, 4.99",
                "EUROPEAN,  3,  500.00, 7.49,  true,  0.00",
                "EUROPEAN,  3,  50.00,  7.49,  true,  0.00",
                "EUROPEAN,  3,  49.99,  7.49,  false, 7.49",
        })
        void qualifyingOrderAtOrAbove50GetsFreeShipping(
                String zone, String weightKg, String orderTotal, String expectedZonedRate,
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
    @DisplayName("Must never make shipping free for international orders, whatever the order total")
    class NoFreeShippingForInternationalOrders {

        @ParameterizedTest(name = "The one where a {0}kg international parcel on a £{1} order has free shipping applied {2} and costs £{3}")
        @CsvSource({
                "3, 500.00, false, 12.48",
                "3, 50.00,  false, 12.48",
        })
        void internationalOrdersNeverGetFreeShipping(
                String weightKg, String orderTotal,
                boolean expectedFreeShippingApplied, String expectedTotalCost) {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": %s, "zone": "INTERNATIONAL", "orderTotal": %s }
                            """.formatted(weightKg, orderTotal))
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
        @DisplayName("The one where a 3kg European parcel on a £500.00 order shows base rate £4.99, multiplier ×1.5, zoned rate £7.49, free shipping applied, and total cost £0.00")
        void europeanFreeShippingStillShowsEveryStepOfTheBreakdown() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "EUROPEAN", "orderTotal": 500.00 }
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
                    .isEqualByComparingTo("1.5");
            assertThat(result).bodyJson()
                    .extractingPath("$.breakdown.zonedRate")
                    .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                    .isEqualByComparingTo("7.49");
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
        @DisplayName("The one where a 3kg domestic parcel on a £49.99 order shows free shipping not applied and total cost £4.99")
        void belowThresholdShowsFreeShippingNotApplied() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 49.99 }
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
        @DisplayName("The one where a domestic order total of £49.995 is under £50.00, so it does not qualify and costs £4.99")
        void orderTotalJustUnderThresholdIsNotRoundedUp() {
            MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                    .header("X-API-Key", TestApiKeys.USER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "weightKg": 3, "zone": "DOMESTIC", "orderTotal": 49.995 }
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
