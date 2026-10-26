# JVM Platform Module (Spring Boot)

starter / infra / auto-configuration

This module provides **shared infrastructure for all JVM-based services**

(Kotlin / Java / other JVM languages).

It is **NOT a business module**.
It contains **only technical, cross-cutting infrastructure**.

---

## ✅ Purpose of This Module

This module exists to:

- Centralize **Spring / Security / JWT / Observability configuration**
- Avoid **duplication across JVM services**
- Enforce **clean separation between domain and infrastructure**
- Provide **extension points for application-specific adapters**
- Standardize **HTTP, security, and technical infrastructure**

It is a **JVM-only platform**.
It is **NOT a platform for Rust / TypeScript / other non-JVM services**.

---

## ✅ What Belongs Here

Only **technical, cross-cutting infrastructure** that can be shared across
multiple JVM applications:

### 🔐 Security & Auth

- `SecurityFilterChain`
- `JwtEncoder`, `JwtDecoder`
- OAuth2 / OIDC configuration
- Security filters
- Extension points like:
  - `HttpAuthContributor`

### 🌍 Web / HTTP

- Global exception handlers
- Servlet / Web filters
- CORS configuration
- Standard headers
- Correlation ID filters

### 📊 Observability

- Logging configuration
- Tracing
- Metrics
- MDC setup

### ⏱️ Time & IDs

- `Clock`
- ID generators
- UUID providers

### 🔁 Resilience

- Retry policies
- Circuit breakers
- Timeouts

### 🧩 Plugin / Extension Mechanisms

- Interfaces used by applications to extend platform behavior
  (e.g. `HttpAuthContributor`)

**Rule of thumb:**

> The platform provides **mechanisms**, not **business rules**.

---

## ❌ What Must NOT Be Here

The platform **must never contain**:

- ❌ Domain entities (`User`, `Order`, `Parcel`, etc.)
- ❌ Use cases (`CreateOrderUseCase`, `PayInvoiceUseCase`)
- ❌ Business rules or decisions
- ❌ Dependencies to any concrete application (`parcel`, `billing`, etc.)
- ❌ Application-specific HTTP controllers (even dev-only ones)
- ❌ Feature logic
- ❌ Anything that depends on business meaning

**This must always be true:**
JVM Application ───────▶ platform
platform ───────▶ (nothing)

yaml
Copy code

---

## ✅ What Stays in Applications (NOT in Platform)

These always belong to each application:

- REST controllers (including `@Profile("dev")` controllers)
- Application-specific security rules
- Domain-to-HTTP mapping
- Any endpoint exposing application API
- Business-related configuration
- Feature flags and domain-driven behavior

Example:

```text
platform ✅  -> JwtEncoder, SecurityCfg
app ❌       -> DevTokenController
✅ Example: Proper Responsibility Split
In platform:
kotlin
Copy code
@Bean
fun jwtEncoder(): JwtEncoder
kotlin
Copy code
interface HttpAuthContributor {
    fun contribute(auth: ...)
}
In application:
kotlin
Copy code
@Component
class ParcelAuthContributor : HttpAuthContributor {
    override fun contribute(auth: ...) {
        auth.requestMatchers("/parcels/**").hasRole("USER")
    }
}
kotlin
Copy code
@RestController
@Profile("dev")
class DevTokenController(private val encoder: JwtEncoder)
✅ Supported Consumers
This module is designed only for:

✅ Kotlin / Spring Boot services

✅ Java / Spring Boot services

✅ Other JVM frameworks if compatible

It is NOT compatible with:

❌ Rust services

❌ TypeScript services

❌ Native-only runtimes

Cross-language sharing must be done via:

OpenAPI / Protobuf

OAuth / JWT standards

Messaging protocols

✅ Design Principles
Infrastructure only

No business meaning

No domain coupling

No upstream dependencies

Extension via interfaces

Spring remains fully isolated from domain layers

⚠️ Warning
If any of the following appears in this module:

Business entities

Use cases

REST endpoints for application features

Application names in package paths

→ The module is being used incorrectly and must be refactored immediately.

✅ Summary
✅ Platform = shared JVM infrastructure

✅ Applications = business + API

✅ Platform exposes mechanisms

✅ Applications define behavior

✅ No domain logic in platform
```
