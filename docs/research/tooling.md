Run #1 – baseline (with bug):
MODE=baseline RATE=200 DURATION=15m k6 run h7_exp00xx_silent_exception_swallowing.js

During this time, record JFR,
analyze: NoSuchMethodError, LambdaForm.compileToBytecode(), Metaspace trend.
Fix the root cause (dependency / classpath).


Run #2 – fixed:
MODE=fixed RATE=200 DURATION=15m k6 run h7_exp00xx_silent_exception_swallowing.js

Same scenario, same parameters,
second JFR for comparison (Thrown Errors, Metaspace, LambdaForms).
Thanks to tags.mode = baseline/fixed in options, both runs will be nicely tagged in metrics / artifacts.


## [jfr]
Start recording:
jcmd <PID> JFR.start name=myrecord settings=profile filename=recording.jfr

Start for a specific duration:
jcmd <PID> JFR.start name=myrecord settings=profile duration=300s filename=recording.jfr

Stop recording and save to file:
jcmd <PID> JFR.stop name=myrecord

If you don't remember the recording names:
jcmd <PID> JFR.check


-Duser.timezone=UTC
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=300s,filename=baseline2.jfr,settings=profile




Programmatic API (JDK 14+)

You can start and stop recordings from code:

var recording = new Recording();
recording.setToDisk(true);
recording.setDestination(Path.of("myrecording.jfr"));
recording.start();

// ... application is running ...

recording.stop();
recording.close();

2. Tooling Layer – What specifically to use

You already have: k6 (load generator) and Spring Boot.
For EXP-0014 you really need 3 levels of observability:
JVM/CPU profile (what consumes CPU time)
Spring/Security metrics (which filters, what durations)
Correlation with k6 load

1.1. JVM / CPU – JFR + JDK Mission Control (base toolset)
This is a must-have because you don't need to install anything other than the JDK.
Enable Java Flight Recorder on your Spring Boot application:
e.g. in dev:
java \
-XX:+FlightRecorder \
-XX:StartFlightRecording=duration=300s,filename=baseline.jfr,settings=profile \
-jar app.jar

After the test:
Open baseline.jfr in JDK Mission Control (JMC),
Look at the CPU Profiling tab → who is on top.
This is enough to see:
whether the security filter chain is in the top 3 CPU consumers,
roughly its share (percentage of samples).
This is the core for EXP-0014.


1.2. More precise CPU profile – async-profiler (optional, but very useful)
If you want nice flamegraphs and more accurate samples:
use async-profiler (open-source, lightweight).
Typical scenario:

# start
./profiler.sh start -e cpu -f security-baseline.svg <PID>

# after test finishes
./profiler.sh stop <PID>


Open security-baseline.svg in a browser,
you see nice flamegraphs – Spring Security filters will be clearly visible.
This tool is great for subsequent HYPs (GC, allocation, lock contention, etc.).


1.3. Spring / Security Level Metrics
To have not only "security in general", but also:
which filter,
what latency/times,
which requests.
You have two options:
Micrometer + Actuator (recommended)
spring-boot-starter-actuator
micrometer-registry-prometheus (or another backend).

You can wrap the filter chain in a Timer:

class MetricsSecurityFilter(
private val meterRegistry: MeterRegistry
) : OncePerRequestFilter() {

    private val timer = Timer.builder("security.filter.chain")
        .description("Time spent in Spring Security filter chain")
        .register(meterRegistry)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        timer.recordCallable {
            filterChain.doFilter(request, response)
        }
    }
}


And from this you extract:

average security time,
p95/p99 per filter chain.

Custom logging / stopwatch – if you don't want Micrometer right away
Simple StopWatch / nanoTime in the filter, DEBUG logs to a separate category.

For EXP-0014, Micrometer is the best investment, because you can later reuse it for all subsequent experiments.


1.4. Load – k6 (already available, just needs standardizing)

For H1 (Security CPU Dominance), you only need:

one clear k6 scenario:

ramp → 200 RPS (target baseline),

GET/POST mix, as planned.

Worthwhile:
keep the script in the repo (/load/k6/exp-0014-security-baseline.js),
describe precisely: which endpoints, what payload.
This is also part of "tooling".

2. How to turn EXP-0014 into a "long-term workshop"

Since this experiment is meta (learning the process + tools), I propose:

2.1. Create the file docs/research/tooling.md
And document:
how you run JFR,
how you run async-profiler (if used),
where the k6 scripts are located,
where you collect metrics (Actuator/Prometheus),
what a standard "performance profile" looks like (command + duration).

For example:

# Research Tooling

## JVM Profiling (JFR)
Command:
java -XX:+FlightRecorder -XX:StartFlightRecording=...

Analysis:
open baseline.jfr in JDK Mission Control

## CPU Flamegraphs (async-profiler)
Command:
./profiler.sh start -e cpu -f <file>.svg <PID>
./profiler.sh stop <PID>

## Load Generation (k6)
Scripts:
/load/k6/exp-0014-security-baseline.js

## Spring Metrics
- Actuator + Micrometer
- key meters:
    - security.filter.chain
    - http.server.requests


This will be your "tooling guide" for all subsequent EXP-XXXX.

2.2. Establish a "standard procedure" for experiments

For example:
Start application with JFR.
Run appropriate k6 script.
If needed, run async-profiler midway through the test.

After the test:
collect .jfr,
collect .svg,

dump metrics from Actuator/Prometheus,

add links/paths to EXP-XXXX.

This becomes your engineering ritual.

2.3. Treat EXP-0014 as a "template experiment"

Meaning:

EXP-0014 not only investigates security,

but also serves as the structural template for:

EXP-0015 (GC),

EXP-0016 (DB),

EXP-0017 (backpressure),

etc.

You can then copy EXP-0014 and only change:

"Driver"

"Hypothesis"

"Load scenario"

"What we measure"

The tools remain the same.

3. Proposed minimal stack to start with

In summary, for your Spring Boot / Kotlin / k6 setup:

For start (must-have):

JFR (built into JDK) + JMC – CPU/GC profile.

k6 – load.

Spring Boot Actuator + Micrometer – minimum metrics.

Extensions (worthwhile, but not from day one):

async-profiler – flamegraphs.

Prometheus/Grafana – if you want dashboards.

This is absolutely sufficient to:

conduct a meaningful EXP-0014,

and establish a "workshop" for all future hypotheses.
