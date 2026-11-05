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

    @Test
    @DisplayName("The one where a 25kg domestic parcel is charged its £11.49 base rate, shown in the breakdown")
    void calculateReportsBaseRateInBreakdown() {
        ShippingCost cost = service.calculate(
                new ShippingRequest(new BigDecimal("25"), DistanceZone.DOMESTIC, new BigDecimal("50.00")));

        assertThat(cost.breakdown().baseRate()).isEqualByComparingTo("11.49");
        assertThat(cost.totalCost()).isEqualByComparingTo("11.49");
    }
}
