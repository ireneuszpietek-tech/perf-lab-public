## EXP: Experiment Execution

### 1. Code
<Java code snippet of the experiment>

### 2. Bytecode Evidence (javap -v)
<paste the important bytecode snippet here – not the entire dump>

### 3. Operational Interpretation
- what invokedynamic/invokevirtual/etc means in this context
- what I expect in JFR
- what JVM behavior should be visible

# EXP-001 — Execution of Experiment for HYP-001

## 1. Code Under Test
...

## 2. Environment & Conditions
...

## 3. Bytecode Excerpts (optional but useful)
...

## 4. Measurement (MEASUREMENT-001)

### JFR Metrics
- Metaspace: 120MB → 145MB → 170MB (linear growth)
- ClassLoad events: +300/s
- ClassUnload: 0
- Allocation rate: 180 MB/s

### k6 Metrics
- avg latency: 55ms
- 95th percentile: 80ms
- errors: 0
- VUs: 50

### JVM/System Metrics
- CPU: 38–42%
- Load average: 1.2
- GC pauses: <5ms, non-dominant

### Observations (still factual, no interpretation!)
- linear class growth visible
- metaspace plateau absent
- performance stable despite class growth