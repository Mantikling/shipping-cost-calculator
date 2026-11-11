package com.example.shipping.model;

import java.math.BigDecimal;

public record OverweightSurcharge(BigDecimal overKg, BigDecimal perKg) {

    /** The surcharge currently in force: £0.50 per kg over 20kg. */
    public static final OverweightSurcharge CURRENT =
            new OverweightSurcharge(new BigDecimal("20"), new BigDecimal("0.50"));
}
