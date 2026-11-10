package com.example.shipping.model;

import java.math.BigDecimal;

public enum DistanceZone {
    DOMESTIC("1.0"),
    EUROPEAN("1.5"),
    INTERNATIONAL("2.5");

    private final BigDecimal multiplier;

    DistanceZone(String multiplier) {
        this.multiplier = new BigDecimal(multiplier);
    }

    public BigDecimal multiplier() {
        return multiplier;
    }
}
