# Quality Report: Weight-Based Shipping Rates

Scope: `docs/specs/weight-tiers.specs.md`, `src/main/java/`, `src/test/java/`, CLAUDE.md and `.claude/rules/`.

Method: the `spec-compliance` and `architecture-guardian` agent definitions are in `.claude/agents/`, but they aren't registered in this session. Their checklists were run through `general-purpose` agents, read-only. Step 3 (mutation analysis) was not run because `target/pit-reports/` does not exist.

## Summary

| Check | Result |
|---|---|
| Spec coverage | All 4 rules and all examples/counter-examples COVERED |
| Architecture | 0 violations, 2 warnings, 5 notes |
| Mutation analysis | Not run (no PIT reports) |

Overall: the feature meets its spec and the layer boundaries hold. The open items are a duplication convention, one unhandled input (null weight), and breakdown fields that no test asserts.

## CRITICAL

None.

## WARNING

1. **Missing `weightKg` returns 500, not 400.** `ShippingCostService.requireAcceptableWeight` calls `weightKg.signum()` on null, which throws `NullPointerException`. The controller doesn't map it. CLAUDE.md requires 400 for validation errors. No test covers this. (Contract, spec-compliance)
   - File: `src/main/java/com/example/shipping/service/ShippingCostService.java`, line ~31
   - Fix: decide the intended behaviour with `/discover`, then drive it with a service-tier test and an acceptance example.

2. **Breakdown intermediates are not asserted at any tier.** Acceptance tests check only `$.breakdown.baseRate`. `totalCost`, `zoneMultiplier`, `zonedRate` and `freeShippingApplied` are never checked. CLAUDE.md says every stage should be visible and testable. (Contract, spec-compliance)
   - File: `src/test/java/com/example/shipping/acceptance/WeightTiersAcceptanceIT.java`
   - Fix: assert the breakdown shape at the HTTP tier, with one representative example.

3. **Assertions duplicated across tiers.** CLAUDE.md says "Don't duplicate the same assertions across both tiers." The service tests repeat the acceptance tier's tier values, surcharge values and rejections. The controller slice test repeats the 2.99 base-rate and 400 checks. (Architecture, spec-compliance)
   - Files: `ShippingCostServiceTest.java`, `ShippingControllerTest.java` (`invalidWeightIsRejectedWithBadRequest`, `calculateReturnsCostBreakdownAsJson`)
   - Fix: keep the exhaustive enumerations at the service tier. Trim the acceptance tier to headline examples plus HTTP-only checks. Remove the duplicate assertions from the controller slice test.

## INFO

4. **Zone multiplier is hard-coded and `zone`/`orderTotal` are ignored.** `calculate()` sets `zoneMultiplier` to `ONE`, `zonedRate` to the base rate, `freeShippingApplied` to `false`, and `totalCost` to the base rate. This matches the scope of this spec, but DOMESTIC is the only zone that gives correct results. Don't release before the zones spec. (Architecture, spec-compliance)

5. **Surcharge is rounded as a total, not on its own.** `baseRate()` adds the surcharge and then rounds. The spec says to round the surcharge to 2dp and then add it. Results are identical for the spec's values (checked at 20.01 → 9.00). (Spec-compliance)

6. **The 400 response has no body.** The client can't see why a request was rejected. CLAUDE.md doesn't specify a body. (Spec-compliance)

7. **Some tests don't trace to a spec rule.** `ShippingControllerTest.calculateReturnsCostBreakdownAsJson` is an API-contract test. `ShippingCostServiceTest.calculateReportsBaseRateInBreakdown` depends on the zone stub. `ShippingApplicationTests.contextLoads` is a smoke test. It sits outside `acceptance/`. (Spec-compliance, architecture)

8. **Controller slice tier is not in CLAUDE.md.** CLAUDE.md defines acceptance and service tiers only. Either document the `@WebMvcTest` tier or fold these tests into acceptance. (Architecture)

9. **No example for 19.99kg.** The spec doesn't state one, so this isn't a gap against the spec. It falls in the 5kg tier at £8.99. (Spec-compliance)

10. **Rate table in `WeightTier` is data.** It's acceptable under `model-rules.md`. Keep it from growing into rules. (Architecture)

## Spec Coverage

| Rule | Example | Status | Test |
|---|---|---|---|
| 1. Flat base rate per tier, lower bound inclusive | 0.5 → 2.99 | COVERED | `baseRateIsFlatPerWeightTierWithLowerBoundInclusive`, `baseRateIsFlatPerWeightTier` |
| | 0.99 → 2.99 | COVERED | same |
| | 1 → 4.99 | COVERED | same |
| | 3 → 4.99 | COVERED | acceptance |
| | 5 → 8.99 | COVERED | both tiers |
| | 10 → 8.99 | COVERED | acceptance |
| | 20 → 8.99 | COVERED | both tiers |
| 2. £0.50/kg over 20kg, pro-rata, HALF_UP | 25 → 11.49 | COVERED | both tiers |
| | 20.5 → 9.24 | COVERED | acceptance |
| | 20.01 → 9.00 (counter-example) | COVERED | both tiers |
| | 50 → 23.99 | COVERED | service `surchargeIsFiftyPencePerKgOverTwenty`, acceptance `fiftyKgParcelIsAccepted` |
| 3. Reject weight ≤ 0 | 0kg → 400 | COVERED | both tiers |
| | −2kg → 400 | COVERED | both tiers |
| | 0.01kg → £2.99 (counter-example) | COVERED | acceptance |
| 4. Reject weight > 50kg | 50.01kg → 400 | COVERED | both tiers |
| | 50kg → £23.99 (counter-example) | COVERED | acceptance, service |

Precision: no `double` or `float` in main or test sources. Every money assertion uses `isEqualByComparingTo`, with exact expected values.

## Step 3: Mutation Analysis

Not run. `target/pit-reports/` does not exist. Run PIT first, then re-run this step.

Note: manual mutation checks were run during the TDD cycles. They changed each boundary and confirmed the test suite fails. See the cycle notes.

## Recommended order

1. Decide null-weight behaviour (`/discover`), then fix it (WARNING 1).
2. Assert the breakdown at the HTTP tier (WARNING 2).
3. Trim duplicate assertions across tiers (WARNING 3).
4. Implement the zones spec before any release (INFO 4).
5. Run PIT to get mutation coverage.
