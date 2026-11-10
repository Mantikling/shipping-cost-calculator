package com.example.shipping.service;

import com.example.shipping.model.DistanceZone;
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

    private static final BigDecimal SURCHARGE_THRESHOLD_KG = new BigDecimal("20");
    private static final BigDecimal SURCHARGE_PER_KG = new BigDecimal("0.50");
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("50");

    public ShippingCost calculate(ShippingRequest request) {
        requireZone(request.zone());
        BigDecimal baseRate = baseRate(request.weightKg());
        BigDecimal zoneMultiplier = request.zone().multiplier();
        BigDecimal zonedRate = baseRate.multiply(zoneMultiplier).setScale(2, RoundingMode.HALF_UP);
        return new ShippingCost(zonedRate,
                new ShippingCost.Breakdown(baseRate, zoneMultiplier, zonedRate, false));
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

    private void requireAcceptableWeight(BigDecimal weightKg) {
        if (weightKg.signum() <= 0) {
            throw new InvalidWeightException("Parcel weight must be greater than 0kg");
        }
        if (weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new InvalidWeightException("Parcel weight must not exceed 50kg");
        }
    }

    private BigDecimal overweightSurcharge(BigDecimal weightKg) {
        return weightKg.subtract(SURCHARGE_THRESHOLD_KG).max(BigDecimal.ZERO).multiply(SURCHARGE_PER_KG);
    }
}
