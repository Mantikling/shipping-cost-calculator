package com.example.shipping.service;

import com.example.shipping.model.DistanceZone;
import com.example.shipping.model.FreeShippingRule;
import com.example.shipping.model.OverweightSurcharge;
import com.example.shipping.model.RateCard;
import com.example.shipping.model.WeightTier;
import com.example.shipping.model.WeightTierRate;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RateCardService {

    public RateCard rateCard() {
        return new RateCard(weightTiers(), OverweightSurcharge.CURRENT, zoneMultipliers(),
                FreeShippingRule.CURRENT);
    }

    private List<WeightTierRate> weightTiers() {
        return Arrays.stream(WeightTier.values())
                .sorted(Comparator.comparing(WeightTier::lowerBound))
                .map(tier -> new WeightTierRate(tier.lowerBound(), tier.baseRate()))
                .toList();
    }

    private Map<DistanceZone, BigDecimal> zoneMultipliers() {
        Map<DistanceZone, BigDecimal> multipliers = new EnumMap<>(DistanceZone.class);
        for (DistanceZone zone : DistanceZone.values()) {
            multipliers.put(zone, zone.multiplier());
        }
        return multipliers;
    }
}
