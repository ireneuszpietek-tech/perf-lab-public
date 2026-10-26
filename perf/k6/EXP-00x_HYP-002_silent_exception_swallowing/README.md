# EXP-00xx — H-07 Silent Exception Swallowing and JVM Degradation

Related hypothesis:
- H-07 — Silent Exception Swallowing and JVM-Level Degradation (Metaspace Growth)

Purpose:
- Compare baseline JVM behavior with swallowed NoSuchMethodError
- vs. fixed dependency (no hidden runtime errors)

How to run:
- `k6 run h7_exp00xx_baseline_vs_fixed.js --env-file=...`

Artifacts:
- JFR recordings (before/after)
- JMC screenshots (Thrown Errors, Metaspace Live Set Trend)
- Analysis in docs/experiments/EXP-00xx_...
