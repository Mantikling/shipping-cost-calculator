package com.example.shipping.model;

import java.math.BigDecimal;

public record ShippingRequest(BigDecimal weightKg, DistanceZone zone, BigDecimal orderTotal) {
}
