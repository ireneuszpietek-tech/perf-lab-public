
## Hypothesis H-001 — Silent Exception Swallowing and JVM-Level Degradation (Metaspace Growth)

## Driver
During JVM-level profiling (JFR) under baseline load testing, the system exhibited
repeated NoSuchMethodError occurrences that did not appear in application logs
and were silently swallowed by framework-level code (Spring / Jackson / Log4j).

These hidden runtime errors correlated with:
repeated LambdaForm.compileToBytecode() events (dynamic bytecode generation),
sustained growth of the JVM Metaspace Live Set,
JMC alerts marking Thrown Errors and Metaspace Live Set Trend as critical.

Because silent exception swallowing hides failures from developers and standard logs,
but still drives real JVM-level behavior (classloading, bytecode generation, CPU usage),
this failure mode must be understood, isolated, and quantified.

## Rationale:

Hidden errors can trigger fallback paths that increase CPU usage or introduce non-obvious control-flow.

Silent swallowing of runtime errors (especially Error, not Exception) can cause
unbounded dynamic bytecode generation, increasing Metaspace usage and eventually leading to OOM Metaspace.

Performance measurements, GC tuning, and concurrency experiments become invalid if
invisible framework-level failures distort JVM behavior.

Understanding this failure mode is essential to maintaining experimental integrity
for all JVM-based performance research on the logistics-core-system platform.

Hypothesis

Silent exception swallowing (especially of runtime errors such as NoSuchMethodError)
causes JVM-level degradation characterized by:

increased dynamic bytecode generation (e.g., LambdaForms, MethodHandles), and

a sustained upward trend in Metaspace Live Set,

which disappears once the swallowed error is eliminated (e.g., dependency mismatch fixed).

In short:

If a runtime error is swallowed, JVM-level degradation emerges;
if the root-cause is removed, degradation subsides.

This is a causal hypothesis, not merely correlational.

Expected Impact

If H-07 is true:

eliminating the swallowed error (e.g., fixing dependency mismatch)
will reduce or eliminate LambdaForm generation events,
and stabilize Metaspace growth.

JMC alerts for

Thrown Errors

Metaspace Live Set Trend
will return to normal (green/yellow) levels.

Future performance experiments will have a more reliable baseline without JVM-level distortion.

Architectural decisions (e.g., dependency governance, strict version alignment, or shading)
may be justified to prevent recurrence.

If H-07 is false:

Metaspace growth and LambdaForm generation occur independently of swallowed errors,

JVM degradation is caused by unrelated mechanisms (e.g., heavy proxy generation, DevTools reloaders, instrumentation agents),

fixing the swallowed error will not materially affect JVM behavior.

Confidence

Low to Moderate.

The correlation between hidden runtime errors and dynamic bytecode generation is observed,
but the causal relationship must be validated experimentally by removing the error
and comparing JVM profiles before/after.

Experiment (link: EXP-00xx)
Method

Baseline (Version A — current state):

Run load test (100–200 RPS, simple request mix).

Record:

JFR with:

Java Exceptions / Errors,

Classloading,

Metaspace statistics,

MethodHandle & LambdaForm events,

CPU samples.

Observe:

frequency of NoSuchMethodError,

count and distribution of LambdaForm.compileToBytecode() events,

Metaspace Live Set Trend.

Intervention:

Fix the root cause of swallowed runtime error
(e.g., remove inconsistent dependency, align versions, eliminate shadowed classes).

Version B — after fix:

Repeat the exact test (same JFR settings, same load).
Collect the same artifacts.

Artifacts to Collect

JFR recordings (before & after fix)

JMC alerts (Thrown Errors, Metaspace Trend)

Dynamic bytecode generation metrics (LambdaForm events)

Classloader statistics

Metaspace usage over time

CPU flamegraphs

Metrics
Primary Metrics

Frequency of NoSuchMethodError events

Count of LambdaForm.compileToBytecode() events

Metaspace Live Set Trend slope (before vs after)

Class metadata allocation rate

Secondary Metrics

CPU overhead related to MethodHandles/LambdaForms

GC behavior (Metaspace reclamation events)

Dynamic class count (number of generated classes)

Falsification Conditions

H-07 is Confirmed if:

Hidden runtime errors (NoSuchMethodError) correlate with:

high LambdaForm generation,

Metaspace growth,

AND after eliminating the swallowed error:

LambdaForm events significantly decrease,

Metaspace trend flattens or stabilizes,

JMC alerts disappear or downgrade in severity.

H-07 is Falsified if:

Silent errors occur but do not correlate with LambdaForm generation or Metaspace growth,

OR fixing the swallowed error does not change JVM-level behavior,

OR Metaspace trend remains high due to unrelated mechanisms.

Decision Path (to be completed in CON-00xx / ADR-00xx)
IF H-07 is confirmed

We conclude:

Silent exception swallowing represents a hidden JVM degradation path.

Dependency mismatches or fallback paths can silently corrupt JVM performance characteristics.

Future R&D pipelines require:

strict dependency alignment,

version verification tooling,

stronger observability for Error-level throw events.

Potential ADRs:

ADR-00xx_JVM_Diagnostics_Integrity_Requirements

ADR-00xx_Dependency_Consistency_and_Error_Surface_Control

ADR-00xx_Preventing_Dynamic_Bytecode_Inflation

Next Steps:

Quantify the exact overhead in H-08 (dynamic bytecode cost analysis).

Evaluate the need for a "Strict Runtime Mode" (fail-fast on runtime errors).

IF H-07 is falsified

We conclude:

Silence exception swallowing is not the cause of JVM degradation.

Metaspace or LambdaForm behavior stems from other factors:

proxy generation,

Spring Security or WebMvc internals,

instrumentation,

DevTools reloaders,

misconfigured classloaders.

Next Steps:

Initiate H-08 on dynamic class generation sources,

Investigate classloader graph and proxy growth,

Establish new hypotheses regarding JVM Metaspace pressure.

Documentation Artifacts to Produce

EXP-00xx: full experiment protocol and results

JFR Before/After comparisons

Metaspace trend graph

LambdaForm frequency graph

CON-00xx: formal outcome

ADR-00xx if architectural decisions follow from the findings