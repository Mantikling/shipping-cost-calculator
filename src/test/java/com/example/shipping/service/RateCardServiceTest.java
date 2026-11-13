package com.example.shipping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shipping.model.DistanceZone;
import com.example.shipping.model.RateCard;
import com.example.shipping.model.WeightTierRate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateCardServiceTest {

    private final RateCardService service = new RateCardService();

    @Test
    @DisplayName("The one where the rate card lists the weight tiers £2.99 from 0kg, £4.99 from 1kg and £8.99 from 5kg")
    void rateCardListsTheWeightTiersInAscendingOrder() {
        RateCard rateCard = service.rateCard();

        List<WeightTierRate> tiers = rateCard.weightTiers();
        assertThat(tiers).hasSize(3);
        assertTier(tiers.get(0), "0", "2.99");
        assertTier(tiers.get(1), "1", "4.99");
        assertTier(tiers.get(2), "5", "8.99");
    }

    @Test
    @DisplayName("The one where the rate card lists a £0.50 per kg surcharge over 20kg")
    void rateCardListsTheOverweightSurcharge() {
        RateCard rateCard = service.rateCard();

        assertThat(rateCard.overweightSurcharge().overKg()).isEqualByComparingTo("20");
        assertThat(rateCard.overweightSurcharge().perKg()).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("The one where the rate card lists free shipping for domestic and European orders of £50.00 or more")
    void rateCardListsTheFreeShippingRule() {
        RateCard rateCard = service.rateCard();

        assertThat(rateCard.freeShipping().zones())
                .containsExactly(DistanceZone.DOMESTIC, DistanceZone.EUROPEAN);
        assertThat(rateCard.freeShipping().minimumOrderTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("The one where the rate card lists the zone multipliers domestic ×1.0, European ×1.5 and international ×2.5")
    void rateCardListsTheZoneMultipliers() {
        RateCard rateCard = service.rateCard();

        assertThat(rateCard.zoneMultipliers()).hasSize(3);
        assertThat(rateCard.zoneMultipliers().get(DistanceZone.DOMESTIC)).isEqualByComparingTo("1.0");
        assertThat(rateCard.zoneMultipliers().get(DistanceZone.EUROPEAN)).isEqualByComparingTo("1.5");
        assertThat(rateCard.zoneMultipliers().get(DistanceZone.INTERNATIONAL)).isEqualByComparingTo("2.5");
    }

    private static void assertTier(WeightTierRate tier, String expectedFromKg, String expectedRate) {
        assertThat(tier.fromKg()).isEqualByComparingTo(expectedFromKg);
        assertThat(tier.rate()).isEqualByComparingTo(expectedRate);
    }
}
