package com.example.shipping.service;

import com.example.shipping.model.DistanceZone;
import com.example.shipping.model.FreeShippingRule;
import com.example.shipping.model.OverweightSurcharge;
import com.example.shipping.model.ShippingCost;
import com.example.shipping.model.ShippingRequest;
import com.example.shipping.model.WeightTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class ShippingCostService {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("50");
    private static final BigDecimal FREE_SHIPPING_COST = new BigDecimal("0.00");

    public ShippingCost calculate(ShippingRequest request) {
        requireZone(request.zone());
        requireOrderTotal(request.orderTotal());
        BigDecimal baseRate = baseRate(request.weightKg());
        BigDecimal zoneMultiplier = request.zone().multiplier();
        BigDecimal zonedRate = baseRate.multiply(zoneMultiplier).setScale(2, RoundingMode.HALF_UP);
        boolean freeShippingApplied = qualifiesForFreeShipping(request);
        BigDecimal totalCost = freeShippingApplied ? FREE_SHIPPING_COST : zonedRate;
        return new ShippingCost(totalCost,
                new ShippingCost.Breakdown(baseRate, zoneMultiplier, zonedRate, freeShippingApplied));
    }

    private boolean qualifiesForFreeShipping(ShippingRequest request) {
        FreeShippingRule rule = FreeShippingRule.CURRENT;
        return request.zone() == rule.zone()
                && request.orderTotal().compareTo(rule.minimumOrderTotal()) >= 0;
    }

    public BigDecimal baseRate(BigDecimal weightKg) {
        requireAcceptableWeight(weightKg);
        return tierFor(weightKg).baseRate()
                .add(overweightSurcharge(weightKg))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private WeightTier tierFor(BigDecimal weightKg) {
        return Arrays.stream(WeightTier.values())
                .filter(tier -> weightKg.compareTo(tier.lowerBound()) >= 0)
                .max(Comparator.comparing(WeightTier::lowerBound))
                .orElseThrow(() -> new IllegalStateException("No weight tier for " + weightKg + "kg"));
    }

    private void requireZone(DistanceZone zone) {
        if (zone == null) {
            throw new InvalidZoneException("Destination zone is required");
        }
    }

    private void requireOrderTotal(BigDecimal orderTotal) {
        if (orderTotal == null) {
            throw new InvalidOrderTotalException("Order total is required");
        }
        if (orderTotal.signum() < 0) {
            throw new InvalidOrderTotalException("Order total must not be negative");
        }
    }

    private void requireAcceptableWeight(BigDecimal weightKg) {
        if (weightKg.signum() <= 0) {
            throw new InvalidWeightException("Parcel weight must be greater than 0kg");
        }
        if (weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new InvalidWeightException("Parcel weight must not exceed 50kg");
        }
    }

    private BigDecimal overweightSurcharge(BigDecimal weightKg) {
        OverweightSurcharge surcharge = OverweightSurcharge.CURRENT;
        return weightKg.subtract(surcharge.overKg()).max(BigDecimal.ZERO).multiply(surcharge.perKg());
    }
}
