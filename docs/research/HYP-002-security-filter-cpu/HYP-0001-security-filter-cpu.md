# Hypothesis H1 — Security Filter Chain CPU Dominance

## Driver
Spring Security (JWT-based, stateless) is currently enabled in the baseline request flow  
to provide authentication and authorization for all incoming HTTP requests.

Before we introduce domain logic, persistence, messaging, or distributed workflows,  
we must understand the *baseline* CPU cost of the security filter chain.

Rationale:

- If Spring Security consumes a significant share of CPU time,  
  it will distort future performance experiments (DB, GC, allocations, domain logic).
- If its cost is small or moderate, then enabling it during early R&D work is safe.
- This measurement is required to ensure that all future latency and throughput tests are interpreted correctly.

Therefore, we must quantify the relative CPU impact of Spring Security under a simple 200 RPS load.

---

## Hypothesis
**Under a 200 RPS baseline load, the Spring Security filter chain (aggregated across all filters)  
is one of the top three CPU-consuming components in the JFR profile.**

This is a *rank-based hypothesis*, not a numeric threshold.  
Its goal is to determine whether Spring Security should be treated as a “major performance center”  
before introducing further system complexity.

---

## Expected Impact
If H1 is true, we expect:

- the security filter chain to dominate a significant portion of CPU time,
- early performance experiments to require a “research mode” with security minimized or bypassed,
- further work on:
    - per-filter breakdown,
    - token processing overhead,
    - unnecessary filters,
    - opportunities for caching or narrowing security application.

If H1 is false:

- security overhead is not dominant,
- it is safe to keep Spring Security enabled in most R&D pipelines,
- performance bottlenecks will more likely come from domain logic, I/O, serialization, or GC.

---

## Confidence
**Low.**  
This is an exploratory hypothesis with no prior measurements.
The goal is to establish an initial “order-of-magnitude” understanding before defining numeric thresholds in H2.

---

## Experiment (link: EXP-0014)
**Load test method:**

- k6 scenario
    - ramp-up: 10 → 100 → 200 RPS
    - mix: 70% `GetShipment`, 30% `CreateShipment`
- record:
    - **JFR** (CPU, allocations, threads, wall-clock)
    - **per-filter execution time** (Spring metrics or custom timing)

**Artifacts to collect:**

- CPU flame graph
- percentage contribution of:
    - security filter chain (total and per filter),
    - Tomcat request parsing + IO,
    - controller + serialization,
    - domain logic (if present),
    - GC and runtime overhead.

---

## Metrics
Primary:

- CPU sample ranking in JFR (top N consumers)
- aggregated CPU sample count for Spring Security filters
- relative position of security chain vs:
    - Tomcat,
    - Controller layer,
    - Serialization,
    - Domain logic,
    - JVM runtime / GC activity.

Secondary:

- wall-clock contribution per request
- per-filter cost breakdown (if measurable)

---

## Falsification Conditions
Hypothesis H1 is:

**Confirmed if**  
Spring Security (aggregated filters) appears in the **TOP 3 CPU-consuming components** in the JFR profile under 200 RPS.

**Falsified if**  
Security does **not** appear in the TOP 3  
or  
its CPU contribution is clearly minor relative to other components.

Numeric thresholds will be introduced in H2 once we know the order of magnitude.

---

## Decision Path (to be completed in CON-001 and ADR if required)

### IF H1 is confirmed (security ∈ TOP 3 CPU)
We conclude:

- Spring Security introduces significant CPU overhead.
- Keeping security enabled during general R&D will distort measurements.
- Early performance work must provide a **“research mode”** where security is minimized or bypassed.

Next steps:

1. Document results in CON-001.
2. Run dedicated experiment:
    - H2: “Quantifying exact CPU % of Security Filter Chain”
3. Potential ADR:
    - **ADR-00xx_Research_Mode_Security_Bypass**
    - **ADR-00xx_Security_Filter_Performance_Strategy**

---

### IF H1 is falsified (security ∉ TOP 3 CPU)
We conclude:

- Security is not a dominant cost center.
- It can remain enabled for most R&D experiments.
- CPU bottlenecks should be sought elsewhere.

Next steps:

1. Document results in CON-001.
2. Proceed to next hypothesis:
    - **H2 — Domain / Persistence Layer Baseline Cost**

---

## Documentation Artifacts to Produce
- EXP-0014: full experiment log
- JFR flamegraph screenshot(s)
- CON-001: formal conclusion
- (Optional) ADR if this experiment drives an architectural decision
