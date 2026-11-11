package com.example.shipping.model;

import java.math.BigDecimal;

public record FreeShippingRule(DistanceZone zone, BigDecimal minimumOrderTotal) {

    /** The rule currently in force: domestic orders of £75.00 or more ship free. */
    public static final FreeShippingRule CURRENT =
            new FreeShippingRule(DistanceZone.DOMESTIC, new BigDecimal("75.00"));
}
