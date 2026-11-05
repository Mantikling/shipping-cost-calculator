package com.example.shipping.model;

import java.math.BigDecimal;

public enum WeightTier {
    HEAVY("5", "8.99"),
    MEDIUM("1", "4.99"),
    LIGHT("0", "2.99");

    private final BigDecimal lowerBound;
    private final BigDecimal baseRate;

    WeightTier(String lowerBound, String baseRate) {
        this.lowerBound = new BigDecimal(lowerBound);
        this.baseRate = new BigDecimal(baseRate);
    }

    public BigDecimal lowerBound() {
        return lowerBound;
    }

    public BigDecimal baseRate() {
        return baseRate;
    }
}
