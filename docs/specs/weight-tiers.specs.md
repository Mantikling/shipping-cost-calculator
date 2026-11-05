# Weight-Based Shipping Rates

**As an online retailer, I want shipping costs calculated from parcel weight using tiered pricing so that heavier parcels are charged appropriately.**

## Rules and Examples

### Rule: Must charge a flat base rate for the weight tier the parcel falls in, with each tier's lower bound inclusive

| Weight (kg) | Base rate |
|---|---|
| 0.5 | £2.99 |
| 0.99 | £2.99 |
| 1 | £4.99 |
| 3 | £4.99 |
| 5 | £8.99 |
| 10 | £8.99 |
| 20 | £8.99 |

Boundary rows are 0.99 → 1 and 5. A parcel exactly on a lower bound moves up to the higher tier. At 20kg there are 0kg over 20, so the price is still £8.99.

---

### Rule: Must add £0.50 per kg over 20kg to the £8.99 base rate, pro-rata for fractions

The surcharge is £0.50 × (weight − 20), rounded to 2 decimal places, HALF_UP.

| Weight (kg) | Base rate |
|---|---|
| 25 | £11.49 |
| 20.5 | £9.24 |
| 20.01 | £9.00 |

- **Counter-example:** The one where a 20.01kg parcel owes £0.005 in surcharge. That rounds HALF_UP to £0.01, so it costs £9.00, not £8.99.

---

### Rule: Must reject a parcel weight of zero or less

- **Example:** The one where a 0kg parcel is rejected as invalid.
- **Example:** The one where a −2kg parcel is rejected as invalid.
- **Counter-example:** The one where a 0.01kg parcel is accepted and charged £2.99.

---

### Rule: Must reject a parcel weight over 50kg

- **Example:** The one where a 50.01kg parcel is rejected as too heavy.
- **Counter-example:** The one where a 50kg parcel is accepted and charged £8.99 + 30 × £0.50 = £23.99.

---

## Resolved decisions (for implementation)

- **Fractional kg over 20:** Pro-rata (£0.50 × exact kg over 20), rounded to 2dp HALF_UP.
- **Maximum weight:** 50kg inclusive. Anything over 50kg is rejected.
- **Out of scope:** Zone multipliers and free shipping. They are covered in other specs and applied after the base rate.
