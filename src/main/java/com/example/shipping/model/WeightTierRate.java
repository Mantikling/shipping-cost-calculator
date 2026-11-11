package com.example.shipping.model;

import java.math.BigDecimal;

public record WeightTierRate(BigDecimal fromKg, BigDecimal rate) {
}
