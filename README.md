# Bank Statement Service — Phase 1

Phase 1 scope: data model (entities), repositories, and seed data only.
No business logic (fee/interest/statement generation) yet — that's Phase 2 onward.

## What's included
- Entities: `User`, `Account`, `Transaction`, `Fee`, `InterestAccrual`, `Statement`, `StatementLine`
  (in `src/main/java/com/bank/statements/model`)
- Repositories for each entity (`src/main/java/com/bank/statements/repository`)
- H2 file-based database config (`src/main/resources/application.yml`)
- Seed data covering 2 users, 4 accounts (savings/credit/wallet, plus one account
  with zero transactions), and ~2 months of transaction history (`data.sql`)

## Notes on the model
- `User` and `Account` intentionally have **no `created_at`** field, per requirement.
- `Account.status` (ACTIVE / INACTIVE / FROZEN / CLOSED) controls which accounts are
  picked up by the monthly batch job in later phases.
- `Transaction` table is mapped to `account_transaction` in the DB, since `TRANSACTION`
  is a reserved SQL keyword in H2.
- `Fee` and `InterestAccrual` are separate tables from `Transaction` — statement totals
  will combine all three at generation time (Phase 4).
- `Statement.status` distinguishes `GENERATED` (final, past month) vs `PROVISIONAL`
  (current in-progress month, computed live) — logic for this comes in Phase 4.
- Unique constraints are defined at the entity level to prevent duplicate fee/interest/
  statement generation for the same account + period.

## Running locally
Requires Java 17+ and Maven, with access to Maven Central to resolve dependencies
(this sandbox environment could not download dependencies or compile the project,
so please verify the build on your machine).

```bash
mvn spring-boot:run
```

- App starts on `http://localhost:8080`
- Seed data loads automatically on startup via `DataSeeder`
- All seeded users share the password `password123` for login testing:
  ```
  POST /api/auth/login
  { "email": "asha.rao@example.com", "password": "password123" }
  ```
- The H2 web console is **disabled by default** (security). To enable it locally:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```
  Then visit `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:file:./data/bankstatement`,
  username `sa`, blank password. Never enable the `dev` profile anywhere reachable
  outside your own machine.

## Frontend
A plain HTML/CSS/JS app (no build step, no framework) lives in `frontend/`.
It talks to the backend via `fetch` calls to `http://localhost:8080`.

**Run it:**
1. Start the backend first (`mvn spring-boot:run`).
2. Serve the `frontend/` folder with any static file server, e.g.:
   ```bash
   cd frontend
   python3 -m http.server 5500
   ```
   Then open `http://localhost:5500`.
   (Opening `index.html` directly via `file://` also works in most browsers,
   since the API calls are absolute URLs to `localhost:8080` and CORS is
   already configured to allow it.)
3. Log in with any seeded demo user (see the login screen for the list) —
   password is `password123` for all of them.

**What it covers:** login, account picker, viewing any month's statement
(including the live PROVISIONAL current month), statement history sidebar,
password-protected PDF download, and an admin panel (visible only to the
admin user) to manually trigger the monthly batch job.

If you change the backend port or deploy it elsewhere, update `API_BASE` at
the top of `frontend/app.js`.

## Known security items (flagged, not yet fixed)
These were identified during a security review but need a product/infra
decision or additional scope before fixing:
- **No brute-force protection on `/api/auth/login`** — add rate limiting or
  account lockout before production.
- **No token revocation** — JWTs can't be invalidated before their 1hr
  expiry (logout, role change, disabled account). Standard stateless-JWT
  trade-off; consider a short-lived access token + refresh token + denylist
  if this becomes a requirement.
- **CORS allows any origin** (`allowedOriginPatterns: ["*"]`) — fine for local
  dev with a Bearer-token API (no cookies/credentials involved), but narrow
  this to your actual frontend's origin before deploying anywhere real.
- **Admin batch endpoint returns raw exception messages** in
  `BatchRunSummary.failureDetails` — low risk (admin-only), but consider
  sanitizing before returning to any client.
- **`User.roles` is free-text** (e.g. "ADMIN"), not an enum/lookup table —
  a typo in stored role data silently breaks `@PreAuthorize` role checks.

## Fixed during security review
- Missing/invalid JWT now returns a proper JSON 401 (`JwtAuthenticationEntryPoint`)
  instead of Spring Security's default blank 403.
- Requesting another user's account now returns the same 404 as a
  non-existent account, instead of a distinguishable 403 (was an IDOR/
  account-enumeration risk).
- H2 console disabled by default; only enabled via the `dev` Spring profile.

## PDF export (Phase 8)
```
GET /api/accounts/{accountId}/statements/{year}/{month}/pdf
Authorization: Bearer <token>
```
- Returns a password-protected PDF download.
- **Password to open it**: first 4 letters of the account holder's name (uppercase)
  + last 4 digits of the account number. E.g. Asha Rao / SAV-0001-0001 -> `ASHA0001`.
  See `StatementPdfPasswordService` if this convention needs to change.
- GENERATED (past-month) statements are rendered once and cached to disk under
  `./data/statement-pdfs/{accountId}/{year}-{month}.pdf` — later requests just
  read the cached file. PROVISIONAL (current-month) statements are re-rendered
  fresh every time, same as the JSON statement endpoint.

## Phase status
- ✅ Phase 1 — Data model & foundation
- ✅ Phase 2 — Fee calculation engine
- ✅ Phase 3 — Interest calculation engine
- ✅ Phase 4 — Statement generation & aggregation
- ✅ Phase 5 — Monthly batch automation
- ✅ Phase 6 — REST API layer
- ✅ Phase 7 — Authentication & security
- ✅ Phase 8 — Password-protected PDF export
- ⬜ Phase 9 — Testing & hardening
