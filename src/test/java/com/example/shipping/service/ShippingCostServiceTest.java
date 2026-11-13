package com.example.shipping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.shipping.model.DistanceZone;
import com.example.shipping.model.ShippingCost;
import com.example.shipping.model.ShippingRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingCostServiceTest {

    private final ShippingCostService service = new ShippingCostService();

    @ParameterizedTest(name = "The one where a {0}kg parcel has a base rate of £{1}")
    @CsvSource({
            "0.5,  2.99",
            "0.99, 2.99",
            "1,    4.99",
            "5,    8.99",
            "20,   8.99",
    })
    @DisplayName("Base rate is flat per weight tier")
    void baseRateIsFlatPerWeightTier(BigDecimal weightKg, BigDecimal expectedBaseRate) {
        assertThat(service.baseRate(weightKg)).isEqualByComparingTo(expectedBaseRate);
    }

    @ParameterizedTest(name = "The one where a {0}kg parcel has a base rate of £{1}")
    @CsvSource({
            "25,    11.49",
            "20.01, 9.00",
            "50,    23.99",
    })
    @DisplayName("Must add £0.50 per kg over 20kg to the £8.99 base rate, pro-rata for fractions")
    void surchargeIsFiftyPencePerKgOverTwenty(BigDecimal weightKg, BigDecimal expectedBaseRate) {
        assertThat(service.baseRate(weightKg)).isEqualByComparingTo(expectedBaseRate);
    }

    @ParameterizedTest(name = "The one where a {0}kg parcel is rejected as invalid")
    @CsvSource({
            "0",
            "-2",
    })
    @DisplayName("Must reject a parcel weight of zero or less")
    void weightOfZeroOrLessIsRejected(BigDecimal weightKg) {
        assertThatExceptionOfType(InvalidWeightException.class).isThrownBy(() -> service.baseRate(weightKg));
    }

    @ParameterizedTest(name = "The one where a {0}kg parcel is rejected as too heavy")
    @CsvSource({
            "50.01",
    })
    @DisplayName("Must reject a parcel weight over 50kg")
    void weightOverFiftyKgIsRejected(BigDecimal weightKg) {
        assertThatExceptionOfType(InvalidWeightException.class).isThrownBy(() -> service.baseRate(weightKg));
    }

    @ParameterizedTest(name = "The one where a {0}kg {1} parcel is multiplied by ×{2} to give £{3}")
    @CsvSource({
            "3, DOMESTIC,      1.0, 4.99",
            "3, EUROPEAN,      1.5, 7.49",
            "3, INTERNATIONAL, 2.5, 12.48",
    })
    @DisplayName("Must multiply the weight-tier base rate by the destination zone's multiplier")
    void zonedRateIsBaseRateTimesZoneMultiplier(
            BigDecimal weightKg, DistanceZone zone, BigDecimal expectedMultiplier, BigDecimal expectedZonedRate) {
        ShippingCost cost = service.calculate(new ShippingRequest(weightKg, zone, new BigDecimal("50.00")));

        assertThat(cost.breakdown().zoneMultiplier()).isEqualByComparingTo(expectedMultiplier);
        assertThat(cost.breakdown().zonedRate()).isEqualByComparingTo(expectedZonedRate);
    }

    @Test
    @DisplayName("The one where a parcel with no zone is rejected as invalid. It is not assumed to be domestic")
    void missingZoneIsRejected() {
        ShippingRequest request = new ShippingRequest(new BigDecimal("3"), null, new BigDecimal("50.00"));

        assertThatExceptionOfType(InvalidZoneException.class).isThrownBy(() -> service.calculate(request));
    }

    @Test
    @DisplayName("The one where a 3kg domestic parcel on a £100.00 order ships free, with the £4.99 zoned rate still shown")
    void domesticOrderOver50GetsFreeShipping() {
        ShippingCost cost = service.calculate(
                new ShippingRequest(new BigDecimal("3"), DistanceZone.DOMESTIC, new BigDecimal("100.00")));

        assertThat(cost.breakdown().zonedRate()).isEqualByComparingTo("4.99");
        assertThat(cost.breakdown().freeShippingApplied()).isTrue();
        assertThat(cost.totalCost()).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest(name = "The one where a {0} order of £{1} has free shipping applied {2} and costs £{3}")
    @CsvSource({
            "DOMESTIC,      50.00,  true,  0.00",
            "DOMESTIC,      49.99,  false, 4.99",
            "DOMESTIC,      49.995, false, 4.99",
            "DOMESTIC,      0.00,   false, 4.99",
            "EUROPEAN,      500.00, true,  0.00",
            "EUROPEAN,      50.00,  true,  0.00",
            "EUROPEAN,      49.99,  false, 7.49",
            "INTERNATIONAL, 500.00, false, 12.48",
            "INTERNATIONAL, 50.00,  false, 12.48",
    })
    @DisplayName("Must make shipping free only for a domestic or European order whose total is £50.00 or more, compared exactly")
    void freeShippingAppliesOnlyToDomesticAndEuropeanOrdersOfAtLeast50(
            DistanceZone zone, BigDecimal orderTotal, boolean expectedFreeShipping, BigDecimal expectedTotalCost) {
        ShippingCost cost = service.calculate(new ShippingRequest(new BigDecimal("3"), zone, orderTotal));

        assertThat(cost.breakdown().freeShippingApplied()).isEqualTo(expectedFreeShipping);
        assertThat(cost.totalCost()).isEqualByComparingTo(expectedTotalCost);
    }

    @ParameterizedTest(name = "The one where a {0} request with no order total is rejected as invalid")
    @CsvSource({
            "DOMESTIC",
            "EUROPEAN",
            "INTERNATIONAL",
    })
    @DisplayName("Must reject a missing order total, whatever the zone")
    void missingOrderTotalIsRejectedForAnyZone(DistanceZone zone) {
        ShippingRequest request = new ShippingRequest(new BigDecimal("3"), zone, null);

        assertThatExceptionOfType(InvalidOrderTotalException.class).isThrownBy(() -> service.calculate(request));
    }

    @Test
    @DisplayName("The one where an order total of −£10.00 is rejected as invalid")
    void negativeOrderTotalIsRejected() {
        ShippingRequest request = new ShippingRequest(
                new BigDecimal("3"), DistanceZone.DOMESTIC, new BigDecimal("-10.00"));

        assertThatExceptionOfType(InvalidOrderTotalException.class).isThrownBy(() -> service.calculate(request));
    }

    @Test
    @DisplayName("The one where a 25kg domestic parcel is charged its £11.49 base rate, shown in the breakdown")
    void calculateReportsBaseRateInBreakdown() {
        ShippingCost cost = service.calculate(
                new ShippingRequest(new BigDecimal("25"), DistanceZone.DOMESTIC, new BigDecimal("20.00")));

        assertThat(cost.breakdown().baseRate()).isEqualByComparingTo("11.49");
        assertThat(cost.totalCost()).isEqualByComparingTo("11.49");
    }
}
