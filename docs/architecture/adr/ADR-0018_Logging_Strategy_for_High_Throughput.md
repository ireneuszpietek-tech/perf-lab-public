ADR-00X: Production Logging Strategy for High-Throughput Systems

Status: Accepted
Date: 2025-11-28
Authors: …
Context: Logging Guidelines / Observability / Performance

### Context

During local load testing (k6), we observed a 20x–30x difference in throughput and response times depending on how the application handled logging.

### Specifically:
- With minimal logging → ~2000 req/s, p95 < 2ms, zero dropped iterations
- With additional INFO/DEBUG logs in application logic → ~90 req/s, p95 40s, heavy iteration drops
- Logging entire collections or large objects (toString on Map, List, DTO trees) produced severe PU/GC pressure and caused the JVM to behave like it was under a self-inflicted DoS
- These findings showed that logging has first-class architectural impact on performance, cost, and reliability, especially in high-RPS scenarios or I/O-bound APIs.
- Therefore, we need a clear decision on how logging should be performed in our system.

### Decision
**We adopt the following Production Logging Strategy:**
- 2.1. INFO logs must be lightweight (O(1))
- INFO logs are always enabled in production.

**Therefore:**
- No logging of full collections, maps, payloads, or large DTOs
- No expensive string building on the request thread
- INFO log messages must be constant-time, ideally simple identifiers (IDs, sizes, states)

Examples of allowed INFO logs:
```
Saved Parcel id=abc-123, storageSize=154
OrderCreated id=..., userId=..., amount=...
```

Examples of forbidden INFO logs:
```
log.info("All items: {}", items)                 # full list
log.info("Storage dump: {}", storage)            # whole map
log.info("Payload: {}", fullRequestBody)         # large JSON
log.info("DTO: {}", dto)                         # deep object tree
```

### DEBUG logs are diagnostic-only and disabled in production
- DEBUG logs will not be emitted in production by default
- They may be selectively enabled per class for short investigations
- Any DEBUG log must be protected with:
if (log.isDebugEnabled) {
    log.debug("...");
}
to avoid building heavy parameters when disabled.

### Heavy diagnostic logs must be conditional
Large or expensive logs (snapshots, partial state dumps) may be emitted only when triggered by an anomaly, e.g.:
- unexpected size thresholds,
- inconsistent state,
- exception paths,
- integrity violations.

### Example:
```
if (storage.size > 10_000) {
    log.warn("Storage exploded, size={}, sample={}", storage.size, storage.entries.take(50))
}
```
This prevents the system from generating expensive logs during normal operation.

### Never log entire collections or objects in request hot-path
**Logging large structures causes:**
- O(n) CPU time per request,
- potentially O(n²) cumulative overhead over time,
- massive GC pressure,
- extremely large log events,
- effective internal DoS when collections grow.

**Instead, we log:**
- the size of the collection,
- a small sample (N elements),
- or identifiers only.

### Business-critical traces should use metrics & tracing, not logs

**For high-RPS services, logs are not the right tool for:**
- counting events,
- monitoring state changes,
- tracking performance,
- diagnosing latency.

**We will use:**
- Micrometer metrics for counters, gauges, timers,
- distributed tracing for call chains,
- logs only for human-readable business events and errors.

2.6. Consider sampling for high-volume diagnostic logs

In rare cases where we want periodic deeper insight:

if (randomChance(0.001)) {
    log.info("State sample: size={}, ids={}", storage.size, storage.keys.take(20))
}


Sampling keeps diagnostic value without overwhelming CPU or log ingestion systems.

2.7. Logging sensitive data is strictly forbidden

To avoid compliance issues and security risks:

No personal data,

No tokens,

No credentials,

No raw request bodies unless explicitly masked.

3. Rationale

This decision is driven by:

3.1. Performance considerations

Logging has proven to add orders of magnitude latency when misused

JVM profiling showed that collection logging caused:

excessive heap allocation,

increased GC frequency,

thread blocking on log appenders,

cascading delays and iteration drops.

3.2. Production operability

Over-logging increases CloudWatch/ELK/S3 costs

Log ingestion and search pipelines have rate limits

Large log events negatively affect parsers and storage

3.3. Developer productivity

Predictable log behavior reduces noise

Consistent patterns ease troubleshooting

Avoids accidental debug-level meltdown during high load

4. Consequences
Positive

Stable performance even at high RPS

Predictable cost and log volume

Easier debugging with targeted snapshots

Less GC pressure and lower CPU usage

Logs remain meaningful and readable

Negative / Trade-offs

Full “state dumps” are no longer available for every request

Debugging issues after the fact requires:

metrics,

traces,

anomaly-triggered snapshots,

or domain-specific audit events

Developers must think consciously about log volume and cost

5. Alternatives Considered
A: Log everything always

Rejected: catastrophic performance, high risk of internal DoS, huge cost.

B: Disable logging completely

Rejected: lack of operational visibility; unacceptable for production.

C: Enable full logging only on-demand by toggling DEBUG

Rejected: toggling after incident is too late; expensive logs must be anomaly-triggered, not level-triggered.

D: Adopt structured logging + metrics + sampling

Accepted: combines visibility with scalability.

6. References

Google SRE Book — Logging & Monitoring Chapter

Uber Engineering Guidelines: Production Logging Best Practices

CNCF Observability Whitepaper

Java Flight Recorder / JDK Mission Control documentation

7. Status & Next Steps

Apply these guidelines to all new and refactored code

Add automated static checks where possible

Monitor production log volume

Periodically review thresholds and sampling strategies