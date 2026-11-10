# Destination Zone Multipliers

**As an online retailer, I want shipping costs adjusted by destination zone so that international deliveries reflect higher logistics costs.**

## Rules and Examples

### Rule: Must multiply the weight-tier base rate by the destination zone's multiplier

The multipliers are Domestic ×1.0, European ×1.5 and International ×2.5.

| Weight (kg) | Base rate | Zone | Multiplier | Zoned rate |
|---|---|---|---|---|
| 3 | £4.99 | Domestic | ×1.0 | £4.99 |
| 3 | £4.99 | European | ×1.5 | £7.49 |
| 3 | £4.99 | International | ×2.5 | £12.48 |

The Domestic row is the boundary: ×1.0 leaves the base rate unchanged.

---

### Rule: Must apply the zone multiplier after the base rate, including any over-20kg surcharge

- **Example:** The one where a 25kg international parcel has a base rate of £11.49 (£8.99 + £2.50 surcharge), so it costs £11.49 × 2.5 = £28.73.

---

### Rule: Must round the zoned rate to 2 decimal places, HALF_UP

- **Example:** The one where a 0.5kg European parcel is £2.99 × 1.5 = £4.485, which rounds up to £4.49.

---

### Rule: Must accept zone names in any letter case

| Zone given | Accepted as |
|---|---|
| EUROPEAN | European |
| european | European |
| International | International |

---

### Rule: Must reject a missing or unknown destination zone

- **Example:** The one where a parcel to zone "MARS" is rejected as invalid.
- **Example:** The one where a parcel with no zone is rejected as invalid. It is not assumed to be domestic.
- **Counter-example:** The one where a 3kg parcel to "DOMESTIC" is accepted and charged £4.99.

---

## Resolved decisions (for implementation)

- **Rounding:** Round the zoned rate once, to 2dp HALF_UP.
- **Missing zone:** Rejected. It does not default to any zone.
- **Zone matching:** Letter case doesn't matter. Spaces around the name are not trimmed.
- **Several invalid inputs:** If the weight and the zone are both invalid, the request is rejected. Which reason gets reported is not specified, and tests must not depend on it.
- **Out of scope:** Free shipping. It is covered in another spec and applied after the zone multiplier.
