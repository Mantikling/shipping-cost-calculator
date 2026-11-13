# Free Shipping for Qualifying Domestic and European Orders

**As an online retailer, I want to offer free shipping for qualifying domestic and European orders, so that I can incentivize larger purchases.**

## Rules and Examples

### Rule: Must make shipping free for a domestic or European order whose order total is £50.00 or more

Free shipping is the last step. It is checked after the base rate and the zone
multiplier, and it waives the whole zoned rate, including any over-20kg surcharge.

| Zone | Weight (kg) | Order total | Zoned rate | Free shipping applied | Total cost |
|---|---|---|---|---|---|
| Domestic | 3  | £100.00 | £4.99  | Yes | £0.00 |
| Domestic | 25 | £80.00  | £11.49 | Yes | £0.00 |
| Domestic | 3  | £50.00  | £4.99  | Yes | £0.00 |
| Domestic | 3  | £49.99  | £4.99  | No  | £4.99 |
| European | 3  | £500.00 | £7.49  | Yes | £0.00 |
| European | 3  | £50.00  | £7.49  | Yes | £0.00 |
| European | 3  | £49.99  | £7.49  | No  | £7.49 |

£50.00 is the boundary row: the threshold is inclusive, so £49.99 does not qualify.

---

### Rule: Must never make shipping free for international orders, whatever the order total

- **Example:** The one where a 3kg international parcel on a £500.00 order costs £12.48.
- **Counter-example:** The one where a 3kg European parcel on a £500.00 order costs £0.00, but the same parcel shipped internationally costs £12.48.

---

### Rule: Must still show the waived charge in the breakdown when shipping is free

- **Example:** The one where a 3kg domestic parcel on a £100.00 order shows base rate £4.99, multiplier ×1.0, zoned rate £4.99, free shipping applied, and total cost £0.00.
- **Example:** The one where a 3kg European parcel on a £500.00 order shows base rate £4.99, multiplier ×1.5, zoned rate £7.49, free shipping applied, and total cost £0.00.
- **Counter-example:** The one where a 3kg domestic parcel on a £49.99 order shows free shipping not applied and total cost £4.99.

---

### Rule: Must compare the order total exactly as given, without rounding it

- **Example:** The one where a domestic order total of £49.995 is under £50.00, so it does not qualify and costs £4.99.

---

### Rule: Must reject a missing or negative order total

- **Example:** The one where a request with no order total is rejected as invalid, for any zone.
- **Example:** The one where an order total of −£10.00 is rejected as invalid.
- **Counter-example:** The one where a 3kg domestic parcel on a £0.00 order is accepted and costs £4.99.

---

## Resolved decisions (for implementation)

- **Qualifying zones:** Domestic and European. International orders never qualify.
- **Threshold:** £50.00, the same for domestic and European orders.
- **Missing order total:** Rejected for every zone. It is not treated as £0.00.
- **Zero order total:** Valid. It doesn't qualify for free shipping.
- **Negative order total:** Rejected.
- **Precision:** The order total is compared exactly as given and never rounded before the check.
- **What the order total covers:** The goods value only, as supplied. Shipping is never added before comparing against £50.00.
- **Processing order:** Step 3 of 3, after the base rate (weight-tiers) and the zone multiplier (distance-zones).
- **Out of scope:** Thresholds by customer tier, promo codes, and free shipping for international orders.
