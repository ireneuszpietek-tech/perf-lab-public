ADR: Standardized Handling of Time, Timezones, and Timestamps

Status: Accepted
Date: 2025-11-23
Decision Makers: Engineering Team
Context: Backend services, distributed systems, multi-timezone users

1. Problem

The system processes requests from users across multiple timezones, while backend services run on infrastructure distributed across regions.
Currently, timestamps appear in different formats and stances (UTC, local offsets, LocalDateTime, ZonedDateTime), causing ambiguity:

Logs from different nodes use different timezone offsets.

Code uses mixed Java date/time types (LocalDateTime, Instant, OffsetDateTime) inconsistently.

Events stored in the DB may not clearly identify their actual point in time.

User requests are interpreted differently depending on server timezone.

This makes debugging, correlation, analytics, and auditing error-prone.

2. Decision
   2.1 System-wide Reference Time = UTC

All backend systems use UTC as the only reference for storing, logging, and exchanging timestamps.

JVM is started with:

-Duser.timezone=UTC


All logging frameworks format timestamps in UTC with ISO-8601:

yyyy-MM-dd'T'HH:mm:ss.SSS'Z'


All backend timestamps represent absolute instants, not local times.

2.2 Internal Time Representation = Instant

Inside the backend code:

All “points in time” use Instant.

LocalDateTime is forbidden for storing or logging events.

Conversion to user-specific timezones is done only when required for domain logic or presentation.

2.3 Domain Timezones Are Explicit

The system distinguishes two types of user timezone information:

Preferred Timezone (optional domain attribute)
A user’s chosen timezone for:

displaying dates in UI,

reports,

business rules tied to user locality.

Request Timezone (per-request, dynamic)
Sent from the client (UI) on every request, e.g. via:

X-User-Timezone: America/New_York


This is used to interpret local timestamps sent by the client.

2.4 Database Storage

Database stores timestamps in UTC only:

TIMESTAMP WITH TIME ZONE normalized to UTC, or

epoch milliseconds/seconds.

Columns representing local business times (e.g. “working hours start at 09:00”) remain local-only but must not be interpreted as instants.

2.5 Conversion Rules

At ingestion:
User-provided local dates must be converted to Instant using the timezone supplied by the client (request_timezone).

At presentation:
Stored Instant → converted to either:

user’s preferred timezone (default), or

client’s current timezone (when relevant).

3. Rationale

UTC is the only stable and unambiguous representation of time across regional servers.

Instant is immune to DST, system timezone changes, and container configuration.

Avoids debugging issues where logs from different hosts cannot be correlated.

Prevents errors caused by implicit use of server timezone (systemDefault()).

Explicit timezones from the user/client avoid incorrect guesses about user locality.

Ensures reproducible event ordering across regions.

4. Consequences
   Positive

Consistent logs across all services and hosts.

Unambiguous timestamps for audits and analytics.

Clear distinction between absolute time (Instant) and local business time.

No reliance on server timezone, eliminating environment drift issues.

Easier correlation in distributed tracing.

Negative / Costs

Requires UI to always send the client’s timezone.

Some refactoring needed to eliminate use of LocalDateTime.

Legacy data may need migration or normalization.

5. Alternatives Considered
   Store local times everywhere

Rejected because:

Ambiguous without a timezone.

Cannot correlate across nodes.

Breaks during DST transitions.

Include timezone offsets inside logs

Rejected because:

Inconsistent offsets across pods/regional nodes.

Harder to aggregate in centralized log systems.

Use ZonedDateTime everywhere

Rejected because:

Adds unnecessary complexity.

Still requires UTC normalization for logs/DB.

6. Adoption Plan

Update JVM defaults to UTC (-Duser.timezone=UTC).

Normalize all access log formats to UTC with Z.

Audit code for disallowed uses of LocalDateTime.

Introduce utilities for Instant + zone conversions.

Update API contracts to include X-User-Timezone header.

Document best practices for new developers.