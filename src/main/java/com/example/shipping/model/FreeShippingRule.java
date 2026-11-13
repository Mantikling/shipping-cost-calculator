package com.example.shipping.model;

import java.math.BigDecimal;
import java.util.List;

public record FreeShippingRule(List<DistanceZone> zones, BigDecimal minimumOrderTotal) {

    /** The rule currently in force: domestic and European orders of £50.00 or more ship free. */
    public static final FreeShippingRule CURRENT =
            new FreeShippingRule(List.of(DistanceZone.DOMESTIC, DistanceZone.EUROPEAN), new BigDecimal("50.00"));
}
