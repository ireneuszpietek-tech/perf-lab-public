---
Driver
↓
Hypothesis (HYP)
↓
Experiment (EXP)
↓
Measurement (RAW DATA)
↓
Validation /     Invalidation (CON)
↙               ↘
New Experiment     New Hypothesis
(refine HYP)        (HYP-n)
↘                   ↘
    ←─── iterative cycle ───→
            ↓
        Decision
            ↓
            ADR
---

1. Driver  
   A system, performance, or behavioral motivation requiring investigation.

2. Hypothesis  
   A falsifiable statement describing expected system behavior.

3. Experiment  
   A controlled setup designed to test the hypothesis, including code, environment, and conditions.

4. Measurement  
   Raw, objective data collected during the experiment (JFR, GC logs, k6 metrics, system statistics).

5. Validation / Invalidation  
   An interpretation comparing results against expected behavior:
    - Confirmation → continue refinement or move to decision
    - Falsification → formulate new hypotheses or adjust experiment design

6. Decision  
   Engineering or architectural action derived from validated findings.

7. ADR  
   A permanent architectural record capturing the decision, rationale, evidence, and alternatives.
