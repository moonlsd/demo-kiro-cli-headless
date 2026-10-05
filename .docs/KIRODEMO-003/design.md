---
id: KIRODEMO-003
title: Order history API
type: design
status: IMPLEMENTED
owner: moonlsd
created: 2026-10-05
updated: 2026-10-05
approvers: []
supersedes: null
---

# Order history API — Detailed Design

> Depends on the approved `requirements.md`. Must comply with everything under
> [`../golden/`](../golden/); call out and justify any deviation. Status model:
> [`../golden/documentation-lifecycle.md`](../golden/documentation-lifecycle.md).

## Overview

This feature adds a single read-only endpoint, `GET /api/v1/orders`, that returns
the authenticated caller's own past orders as a bounded, sorted page. It is
implemented with the standard layered stack: a thin `@RestController` handles HTTP
concerns (validation, mapping, status), an `@Service` owns the business rules
(scope to the authenticated user, cap page size, map entities to DTOs, read-only
transaction boundary), and a Spring Data `@Repository` performs the single
user-scoped, date-sorted, paginated query backed by a composite index.

How it satisfies the requirements:

- **FR-1** (list my orders) and **FR-5** (most-recent-first) — the service issues a
  paginated query filtered by the caller's user id and sorted by `orderDate DESC`,
  mapping each entity to a minimal `OrderSummaryResponse`. An empty history returns
  `200` with an empty page, never `404`.
- **FR-2** (pagination) — the controller accepts `page`/`size`, applies documented
  defaults, and the service caps `size` at a configured maximum; responses carry
  total elements and total pages.
- **FR-3** (user scoping) — the owning user id is derived from the authenticated
  principal and applied as a mandatory query filter at the service boundary
  (default-deny); it is never taken from client input. Unauthenticated callers are
  rejected with `401` before any order data is read.
- **FR-4** (input validation) — `page`/`size` are constrained with Bean Validation;
  violations become the central `400` problem-detail response.

The design introduces no new entity: it reads an existing `orders` table through a
new read-only repository query, so the ordering domain is reused rather than
redefined (per business.md assumptions).

## Architecture & components

Where this fits in the layered architecture (`controller → service → repository`),
root package `com.example.demo`:

- **Controller** — `controller/OrderController`
  - Maps `GET /api/v1/orders`.
  - Binds and validates `page`/`size` query parameters (via a validated
    `OrderPageQuery` DTO or `@Validated` parameters).
  - Obtains the authenticated principal and passes only the resolved user id to the
    service — it never reads a user id from the request body or query string.
  - Maps the service's `Page<OrderSummaryResponse>` into the response
    `PagedResponse<OrderSummaryResponse>` and returns `200`.
  - Contains no business logic and never touches the repository.
- **Service** — `service/OrderHistoryService`
  - `@Transactional(readOnly = true)` business entry point
    `getOrderHistory(userId, pageRequest)`.
  - Enforces the page-size cap and the default sort (`orderDate` descending) by
    constructing the `Pageable` it hands to the repository (controller-supplied sort
    is not honored for this release).
  - Calls the repository with the mandatory user-id filter (default-deny scoping).
  - Maps `Order` entities to `OrderSummaryResponse` DTOs at this boundary; entities
    never leave the service.
- **Repository** — `repository/OrderRepository extends JpaRepository<Order, Long>`
  - Derived query `Page<Order> findByUserId(Long userId, Pageable pageable)` —
    a single parameterized, user-scoped, paginated lookup. Sort is supplied via the
    `Pageable`, so no `@Query` is needed.
- **Domain** — `domain/Order` (entity, maps the existing `orders` table) and
  `domain/OrderStatus` (enum).
- **DTO** — `dto/OrderSummaryResponse` (per-order summary), `dto/PagedResponse<T>`
  (page envelope), `dto/OrderPageQuery` (validated pagination input, optional).
- **Config** — `config/OrderHistoryProperties`
  (`@ConfigurationProperties(prefix = "order-history")`) binds default and maximum
  page size.
- **Exception / error handling** — reuses the application-wide
  `@RestControllerAdvice`; this feature adds no bespoke controller-level
  `try/catch`.

```
Client ──GET /api/v1/orders?page&size (Bearer auth)──> OrderController
                                                          │  resolve principal → userId
                                                          │  validate page/size
                                                          ▼
                                               OrderHistoryService  (@Transactional(readOnly))
                                                          │  cap size, force orderDate DESC
                                                          │  map Order → OrderSummaryResponse
                                                          ▼
                                               OrderRepository.findByUserId(userId, pageable)
                                                          │  parameterized, user-scoped query
                                                          ▼
                                               orders table  (index: user_id, order_date DESC)
```

## API design

Follows [`../golden/api-standards.md`](../golden/api-standards.md): versioned,
noun-based plural resource; JSON only; `camelCase` fields; ISO-8601 UTC timestamps;
Bean Validation on input; errors via the central problem-detail shape (not
redefined here).

| Method | Path | Request | Response | Status codes |
|--------|------|---------|----------|--------------|
| GET | `/api/v1/orders` | Query params `page` (int, 0-based, default `0`, `>= 0`), `size` (int, default `20`, `>= 1`, capped at `100`). Auth via the application's bearer/session mechanism. | `PagedResponse<OrderSummaryResponse>` | `200`, `400`, `401`, `500` |

Notes:

- No `404`: a valid authenticated request always yields a `200` page, empty when the
  user has no orders or the requested page is past the end (FR-1, FR-2).
- No request body (read-only `GET`), so there is no body to validate — only the
  query parameters are constrained.
- `401` is produced by the security layer for unauthenticated callers before the
  controller logic runs (FR-3).

### DTOs

`OrderSummaryResponse` — the minimal summary (FR-1; confidential/PII minimization):

```json
{
  "orderId": 10432,
  "orderDate": "2026-09-18T14:22:05Z",
  "status": "DELIVERED",
  "totalAmount": 129.99,
  "currency": "USD"
}
```

- Contains only identifier, date, status, and total (with its currency). It
  deliberately omits payment instruments, line items, internal flags, and the
  owning `userId` — nothing beyond what the caller needs (api/security standards).

`PagedResponse<T>` — a stable, framework-agnostic page envelope so the response
contract does not leak Spring's internal `Page` serialization:

```json
{
  "content": [ /* OrderSummaryResponse[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 57,
  "totalPages": 3
}
```

`OrderPageQuery` (request binding, validated):

- `page` — `@Min(0)`, defaults to `0`.
- `size` — `@Min(1)`, defaults to `20`; values above the configured maximum are
  capped by the service (not rejected), per FR-2.

### Example

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/orders?page=0&size=20"
```

## Data model & persistence

Follows [`../golden/data-standards.md`](../golden/data-standards.md). This feature
**reads existing data** and adds **no new table**; it adds a read query and the
index that supports it.

### Entity `Order` (maps existing `orders` table)

| Field | Column | Type | Notes |
|-------|--------|------|-------|
| `id` | `id` | `BIGINT` PK (surrogate) | Order identifier returned as `orderId`. |
| `userId` | `user_id` | `BIGINT`, not null | Owning user; the scoping filter. FK to the users table. |
| `orderDate` | `order_date` | `TIMESTAMP WITH TIME ZONE` (UTC), not null | Sort key, returned as `orderDate`. |
| `status` | `status` | `VARCHAR`/enum, not null | Mapped to `OrderStatus`. |
| `totalAmount` | `total_amount` | `NUMERIC(12,2)`, not null | Monetary total; `BigDecimal` in Java. |
| `currency` | `currency` | `CHAR(3)`, not null | ISO-4217 code. |

- `snake_case` columns mapped to `camelCase` fields; plural table name `orders`.
- Monetary values use `BigDecimal`/`NUMERIC` (never floating point).
- Payment/internal columns, if present on the table, are **not mapped** into the
  summary projection and never serialized.

### Query pattern & index (NFR-1)

- Access path: filter by `user_id`, sort by `order_date` descending, paginate.
- Add a composite index to support it:

  ```sql
  CREATE INDEX idx_orders_user_id_order_date
    ON orders (user_id, order_date DESC);
  ```

- This index makes the user-scoped, most-recent-first page an index range scan,
  supporting the p95 < 200 ms target (NFR-1).

### Migrations

- Per data-standards, schema changes ship as versioned, immutable migrations
  (Flyway). This feature contributes **one additive migration** that creates
  `idx_orders_user_id_order_date`. It does not alter columns and does not rely on
  `ddl-auto`.
- If the project has not yet adopted a migration tool, that adoption is a
  prerequisite recorded here; the index must not be created via `ddl-auto`.
- The `orders` table itself is assumed to already exist (owned upstream); this
  feature does not create or backfill it.

### Transactions

- The single read path runs under `@Transactional(readOnly = true)` on the service
  method, giving the driver a read-only hint and a clear boundary. No write
  transactions are introduced.

## Key design decisions

| Decision | Options considered | Choice & rationale |
|----------|--------------------|--------------------|
| Full `controller → service → repository` layering | Controller-only shortcut (as in KIRODEMO-001) vs. full layering | Full layering: this feature has real business rules (user scoping, size capping, DTO mapping) and persistence, so each layer earns its place (architecture-principles). |
| Derive owning user from the authenticated principal | Trust a `userId` query/path param vs. derive from the authenticated identity | Derive from the principal: client-supplied ids would allow trivial cross-user access. The id is resolved server-side and applied as a mandatory filter (FR-3, security-standards default-deny). |
| Spring Data derived query `findByUserId(..., Pageable)` | Derived method vs. hand-written `@Query` | Derived method: the lookup is a simple filter + sort + page, so no `@Query` is needed (data-standards prefers derived queries). Parameterized by construction — no string concatenation. |
| Service builds the `Pageable` (fixed `orderDate DESC`, enforced max size) | Honor arbitrary client sort/size vs. server-controlled sort and capped size | Server-controlled: FR-5 fixes the default sort and FR-2/NFR-5 require a hard page-size cap to bound per-request cost. Arbitrary client sort is out of scope for this release. |
| Oversized `size` is capped (clamped), not rejected | `400` on `size > max` vs. silently cap to max | Cap to max: FR-2 states the effective size is capped at the maximum; this is friendlier and matches the acceptance criteria. Only structurally invalid input (negative/zero/non-numeric) yields `400`. |
| Custom `PagedResponse<T>` envelope | Serialize Spring's `Page`/`PageImpl` directly vs. a stable DTO envelope | Custom envelope: Spring's default `Page` JSON is unstable across versions and leaks internal fields; an explicit DTO gives a stable, documented contract (api-standards). |
| `@ConfigurationProperties` for page sizes | Hardcoded constants / scattered `@Value` vs. a bound properties POJO | Bound POJO: default and max page size are externalized and tunable per environment without code change (architecture/config standards). |
| Minimal `OrderSummaryResponse` projection | Return the full order vs. a minimal summary | Minimal summary: FR-1 and data/security standards require returning only identifier, date, status, and total — never payment or internal fields (data minimization). |

## Error handling

All failures are translated to the central RFC-7807-style problem-detail response by
the application-wide `@RestControllerAdvice`; this feature adds no scattered
`try/catch` in the controller.

| Condition | Mapped by | Status | Client sees |
|-----------|-----------|--------|-------------|
| `page < 0`, `size < 1`, or non-numeric `page`/`size` | Bean Validation (`ConstraintViolationException` / `MethodArgumentTypeMismatchException`) → advice | `400` | Problem detail with per-field validation message (FR-4). |
| Unauthenticated caller | Security layer / authentication entry point | `401` | Generic unauthorized problem detail; no order data (FR-3). |
| Unexpected server/persistence failure | Advice fallback handler | `500` | Generic message; **no** stack trace, SQL, or internals (FR-4, security-standards). |

- A `size` above the configured maximum is **not** an error — it is capped (see FR-2
  and Key design decisions).
- Messages are safe for clients and do not reveal whether another user's data
  exists.

## Security

Beyond the baseline in
[`../golden/security-standards.md`](../golden/security-standards.md):

- **Authentication** — the endpoint is non-public and requires an authenticated
  caller; unauthenticated requests get `401` before any query runs (FR-3, NFR-3).
- **Authorization / scoping (default-deny)** — the owning user id is taken only from
  the authenticated principal and applied as a mandatory repository filter inside
  the service. There is no code path that returns orders for a user id supplied by
  the client, and no "all orders" query exists in this feature. Authorization is
  enforced at the service boundary, not merely in the controller (FR-3, NFR-3).
- **Input handling** — `page`/`size` validated/constrained with Bean Validation;
  all data access is through the parameterized Spring Data query (no string-built
  SQL) (NFR-3).
- **Data protection / minimization** — responses carry only the summary fields;
  payment details and internal fields are never mapped or serialized. Order data is
  treated as confidential/PII (data-standards, NFR-7).
- **No sensitive logging** — order contents, totals tied to identity, and PII are
  never logged (see Observability).
- **Transport** — external traffic is over TLS per the baseline; the app returns the
  minimum necessary data.

## Observability

Follows the `../golden/` observability expectations (NFR-4):

- **Logging** — log order-history access at `INFO` as a business milestone using
  SLF4J parameterized logging, recording the acting user id and the page/size
  requested, plus the resulting `totalElements` count. **Never** log order contents,
  totals, currency, status lists, or any PII. Validation/auth failures are logged by
  the central advice/security layer at `WARN` without sensitive values.
- **Metrics** — rely on the standard Spring Boot / Actuator HTTP server metrics
  (request count, latency, status) exposed via Micrometer; the p95-latency target
  (NFR-1) is tracked from the `http.server.requests` timer for this route. No custom
  metric is required initially.
- **Health** — covered by the existing Actuator `health` endpoint; this feature adds
  no new health indicator and no new single point of failure (NFR-2).
- Actuator exposure stays minimal (only the endpoints already enabled).

## Testing strategy

Aligns with the testing steering; every FR is traceable to a test.

- **Repository slice tests** (`@DataJpaTest`, in-memory or Testcontainers DB):
  - `findByUserId` returns only the given user's rows (FR-3 at the data layer).
  - Results honor the `Pageable` page/size and `orderDate DESC` sort (FR-2, FR-5).
  - Empty result for a user with no orders and for a page past the end (FR-1, FR-2).
- **Service unit tests** (plain JUnit + Mockito, mocked repository):
  - Page size is capped at the configured maximum when a larger `size` is requested
    (FR-2).
  - Default sort `orderDate DESC` is applied when none is given (FR-5).
  - The repository is always called with the principal-derived user id and never a
    client-supplied one (FR-3).
  - `Order` → `OrderSummaryResponse` mapping exposes only the summary fields and no
    payment/internal data (FR-1, data minimization).
- **Web slice tests** (`@WebMvcTest(OrderController.class)` + `MockMvc`, mocked
  service, security filters active):
  - Authenticated `GET /api/v1/orders` → `200` with a well-formed
    `PagedResponse` (FR-1, FR-2).
  - User with no orders → `200` with empty content and `totalElements = 0`, not
    `404` (FR-1).
  - Unauthenticated request → `401`, no body data (FR-3).
  - `page=-1`, `size=0`, and non-numeric values → `400` with the central
    problem-detail shape and per-field messages; no stack trace/internals (FR-4).
- **Integration test** (`@SpringBootTest`, full stack with seeded data):
  - User A sees only A's orders and none of B's (FR-3 end-to-end — the critical
    privacy guarantee).
  - Most-recent-first ordering and pagination metadata verified against seeded rows
    (FR-2, FR-5).

The requirements `Traceability` table is updated with the concrete test names during
implementation.

## Rollout & operations

- **Migration** — one additive Flyway migration creating
  `idx_orders_user_id_order_date`. Index creation on a large existing table should
  use the database's concurrent/online index build where supported to avoid locking
  writes; schedule accordingly.
- **Configuration / env vars** (bound via `OrderHistoryProperties`):

  | Property | Env var | Default | Purpose |
  |----------|---------|---------|---------|
  | `order-history.default-page-size` | `ORDER_HISTORY_DEFAULT_PAGE_SIZE` | `20` | `size` applied when the caller omits it (FR-2). |
  | `order-history.max-page-size` | `ORDER_HISTORY_MAX_PAGE_SIZE` | `100` | Hard cap on effective `size` (FR-2, NFR-5). |

- **Feature flag** — optional: the route may ship behind a simple enablement flag so
  it can be disabled without redeploy; low risk, as the feature is read-only and
  additive.
- **Rollback** — the endpoint is additive and read-only; rollback is redeploying the
  previous image. The index is safe to leave in place; if it must be removed, a
  follow-up migration drops it (migrations are never edited in place).
- **README** — update the endpoint table, configuration section, and project
  structure in the same change that implements this, per the documentation steering.

## Risks & trade-offs

- **Cross-user data leak** (highest risk, from business.md) — mitigated by deriving
  the user id solely from the authenticated principal, applying it as a mandatory
  service-layer filter (default-deny), and verifying with explicit service, slice,
  and end-to-end tests.
- **Unbounded / slow responses** — mitigated by a mandatory, server-enforced maximum
  page size and the supporting composite index (NFR-1, NFR-5).
- **Unstable response contract** — avoided by using an explicit `PagedResponse` DTO
  instead of serializing Spring's internal `Page`.
- **Index build cost on a large table** — mitigated by using an online/concurrent
  index build and scheduling the migration appropriately.
- **Dependency on the existing auth mechanism** — this feature consumes, but does not
  define, authentication. If the project lacks a configured auth mechanism, wiring it
  (so the principal and `401` behavior exist) is a prerequisite rather than part of
  this read feature's logic; it is called out here so the dependency is explicit.

## Deviations from golden standards

No intentional deviations from the golden standards. The design follows the layered
architecture, the API standards (versioned plural resource, JSON/`camelCase`,
central error shape, documented default/max page size), the data standards
(repository-only access, derived parameterized query, read-only transaction,
versioned additive migration, supporting index, `BigDecimal` money, UTC
timestamps), and the security standards (authenticated endpoint, default-deny
service-boundary scoping, Bean Validation, data minimization, no sensitive logging).

Two prerequisites are noted rather than deviations: an authentication mechanism must
exist to supply the principal and `401` behavior, and a schema-migration tool
(Flyway) must be adopted to ship the index — both are required by the golden
standards themselves.

## Implementation notes (prerequisites satisfied in this change)

The two prerequisites above were satisfied concretely so the read feature runs end
to end in this demo application. Neither changes the feature's logic:

- **Authentication** — stateless HTTP Basic against an in-memory user store
  (`SecurityConfig` + `AppUserDetails`). Each user carries the numeric `userId`
  consumed by the service; the `/api/v1/orders` route requires authentication and an
  unauthenticated caller gets the standard `401` problem-detail
  (`ProblemDetailAuthenticationEntryPoint`). In a real deployment this store is
  replaced by the organization's identity provider; the principal contract
  (`AppUserDetails.getUserId()`) is unchanged.
- **Schema / migrations** — Flyway owns the schema (`ddl-auto=none`). `V2` is this
  feature's additive index `idx_orders_user_id_order_date`. Because the demo has no
  upstream `orders` table, a `V1` baseline migration provisions the table so the
  read path is runnable; in production the table is owned upstream and `V1` would
  not be contributed by this feature.
- **Central error handling** — a shared `@RestControllerAdvice`
  (`GlobalExceptionHandler`) plus the `ApiError` record implement the standard
  problem-detail shape for `400`/`401`/`500`, as the API/security standards require.
