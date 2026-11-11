package com.example.shipping.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RateCard(
        List<WeightTierRate> weightTiers,
        OverweightSurcharge overweightSurcharge,
        Map<DistanceZone, BigDecimal> zoneMultipliers,
        FreeShippingRule freeShipping) {
}
