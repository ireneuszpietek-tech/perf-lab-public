# ADR: Time Handling and Time Zone Strategy

## Status
Accepted

## Context

Our system:

- Runs on JVM (Spring Boot / Tomcat).
- Has users in multiple time zones.
- Produces logs (application + infrastructure).
- Persists date-time information in the database.
- Integrates with external systems (APIs, queues, etc.).

Historically, different parts of the system have used different time representations and time zones (e.g. `LocalDateTime`, server-local time, database defaults, etc.). This leads to:

- Ambiguity: it is not clear what "2025-11-23T09:59:31.994" actually means (which time zone?).
- Hard-to-debug issues when correlating logs across services and infrastructure.
- Problems when moving services between servers/pods with different local time configuration.
- Confusion when users are in different time zones and travel frequently.

We need a **clear, consistent and non-breakable** rule for how we handle time.

## Decision

1. **System / JVM time zone MUST be UTC**

    - The JVM for all services MUST run with:
        - `-Duser.timezone=UTC`
    - On startup, the application MUST verify this and fail fast if misconfigured:

      ```java
      if (!ZoneId.systemDefault().equals(ZoneId.of("UTC"))) {
          throw new IllegalStateException("System timezone must be UTC");
      }
      ```

    - This prevents "silent" misconfiguration where one microservice runs in a different time zone.

2. **All stored and logged timestamps are in UTC**

    - All timestamps persisted in the database MUST be:
        - **Instant** (or equivalent) or
        - offset-aware types explicitly stored as UTC.
    - All logs MUST use UTC in ISO-8601 with `Z` suffix, for example:
        - `2025-11-23T08:59:31.994Z`
    - No local offsets (`+01:00`, `+02:00`, etc.) in logs.  
      Logs are a *universal timeline*, not user-facing data.

3. **User time zone is a domain concept**

    - The user’s time zone is stored as a **domain field**, e.g.:
        - `user.timeZone = "Europe/Warsaw"`
    - When we need to present time to a user or accept time from a user:
        - We convert between UTC and the user's time zone explicitly:
          ```java
          Instant now = Instant.now();                 // UTC
          ZoneId userZone = ZoneId.of(userTz);         // e.g. "Europe/Warsaw"
          ZonedDateTime userTime = now.atZone(userZone);
          ```
    - This does **not** depend on server or pod location.
    - If the user travels and changes time zone, this is handled by updating the user’s time zone in the domain model or by providing the current time zone from the client on each request (depending on business rules).

4. **API contracts are UTC-first**

    - Backend–backend APIs MUST exchange timestamps in UTC ISO-8601 with `Z`.
    - Any time zone-aware display for the end user is done at:
        - frontend, or
        - backend presentation layer, based on known user time zone.

5. **Consistency over convenience**

    - We explicitly forbid using `LocalDateTime` for "points in time" (it has no zone/offset).
    - For "points in time", we use:
        - `Instant`, or
        - `OffsetDateTime` with offset = UTC.
    - `LocalDateTime` is allowed only for **pure local concepts** (e.g. "open every day at 09:00 local time") where no absolute time is implied.

## Consequences

### Positive
- **Single source of truth**: Every event in logs and DB is anchored in UTC.
- **Easy correlation**: Logs and events from different services and infrastructures can be correlated without thinking about time zones.
- **Fail-fast safety**: If the JVM is misconfigured (`user.timezone` not UTC), the application fails on startup instead of producing ambiguous timestamps.
- **Clear separation**:
    - Infrastructure / storage / integration → UTC.
    - User-facing representation → (UTC → user time zone) conversion.

### Negative / Trade-offs

- We "lose" the time zone in logs and stored timestamps (everything is normalized to UTC), so:
    - If we need to know *what the user saw* at that moment (e.g. local time on their screen), we must reconstruct it using:
        - event time (UTC) and
        - user time zone at that time (from domain data).
- Slightly more code:
    - We always convert from `Instant`/UTC when displaying to user or parsing user input.
- More discipline needed:
    - Developers must not casually use system default time zone or `LocalDateTime` for absolute timestamps.

## Implementation Notes

- **JVM configuration**

    - Gradle / run config / container:
        - `-Duser.timezone=UTC`

- **Startup validation (fail-fast)**

  ```java
  @Component
  public class TimezoneGuard {

      @PostConstruct
      public void verifySystemTimezone() {
          ZoneId systemZone = ZoneId.systemDefault();
          ZoneId expected = ZoneId.of("UTC");
          if (!systemZone.equals(expected)) {
              throw new IllegalStateException("System timezone must be UTC, but was " + systemZone);
          }
      }
  }
  ```

