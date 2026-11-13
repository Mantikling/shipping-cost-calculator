# API Security with API Key Authentication

**As a platform administrator, I want the shipping API secured with API key authentication, so that only authorized clients can access the service.**

## Rules and Examples

### Rule: Must reject a request to a protected endpoint that carries no API key

A header that's present but blank (empty or only whitespace) counts as no key.

- **Example:** The one where a shipping calculation with no X-API-Key header is rejected as unauthenticated (401, "Missing API key").
- **Example:** The one where a shipping calculation with an empty X-API-Key header is rejected the same way (401, "Missing API key").
- **Counter-example:** The one where the same calculation with a valid USER key is accepted and a 3kg domestic parcel on a £10.00 order costs £4.99.

---

### Rule: Must reject a request whose API key doesn't exactly match a configured key

- **Example:** The one where a calculation with the key "not-a-real-key" is rejected as unauthenticated (401, "Invalid API key").
- **Counter-example:** The one where a valid USER key sent in different letter case is still rejected (401, "Invalid API key"). Keys match exactly.

---

### Rule: Must grant access to each endpoint based on the key's role

| Caller | Calculate shipping | View admin rates |
|---|---|---|
| USER key | 200 | 403 |
| ADMIN key | 200 | 200 |
| No key / invalid key | 401 | 401 |

The USER → admin rates row is the boundary: the caller is authenticated but not authorised, so it gets 403 ("Insufficient role"), not 401. ADMIN can do everything USER can.

---

### Rule: Must return the current rate card to an ADMIN viewing admin rates

- **Example:** The one where an ADMIN sees weight tiers £2.99 (from 0kg), £4.99 (from 1kg) and £8.99 (from 5kg), a £0.50 per kg surcharge over 20kg, zone multipliers domestic ×1.0, European ×1.5 and international ×2.5, and a free-shipping threshold of £50.00 for domestic and European orders.
- **Counter-example:** The one where a USER asking for the same rate card gets 403 and no rates.

---

### Rule: Must check the API key before validating the request

- **Example:** The one where a calculation with no key and a weight of −2kg returns 401, not 400.
- **Counter-example:** The one where the same −2kg calculation with a valid USER key returns 400.

---

### Rule: Must keep the API documentation open to everyone, whatever key is sent

- **Example:** The one where Swagger UI and the OpenAPI description open with no key.
- **Counter-example:** The one where Swagger UI still opens when the request carries an invalid key. The docs ignore the key entirely.

---

### Rule: Must accept any of several keys configured for a role

- **Example:** The one where two USER keys are configured and each can calculate shipping on its own.
- **Counter-example:** The one where a key that was in the USER list and has been removed from configuration is rejected (401).

---

## Resolved decisions (for implementation)

- **Key configuration:** `application.properties` holds a comma-separated list of keys for each role (USER and ADMIN).
- **Blank key:** Treated as missing (401, "Missing API key").
- **Key matching:** Exact and case-sensitive. Keys aren't trimmed before matching.
- **Error body:** JSON `{ "error": "...", "message": "..." }`. "Unauthorized" with "Missing API key" or "Invalid API key" for 401, and "Forbidden" with "Insufficient role" for 403.
- **Check order:** Authentication runs before authorisation, and both run before request validation.
- **Documentation paths:** `/swagger-ui.html`, `/swagger-ui/**` and `/v3/api-docs/**` are public and never look at the key.
- **Admin rates:** `GET /api/admin/rates` is new in this story. It returns the weight tiers, the over-20kg surcharge, the zone multipliers and the free-shipping threshold, all taken from the same values the calculation uses.
- **Existing tests:** The acceptance tests for weight tiers, zones and free shipping must send a valid USER key once security is on.
- **Out of scope:** Key rotation or expiry, rate limiting, editing rates through the API, roles beyond USER and ADMIN, and storing keys anywhere other than configuration.
