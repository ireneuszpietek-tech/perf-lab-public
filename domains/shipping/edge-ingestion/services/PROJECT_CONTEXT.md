# Large File Processing Lab:  Project Context

## 1. Purpose

This repository is a software and systems engineering laboratory for learning how a Java application processes data sets that are much larger than available memory.

The representative workload is:

```text
10 files × 10 GB = 100 GB input
JVM maximum heap = 512 MB
```

The application must derive useful summary information without materializing the complete input, either as raw bytes or as Java objects.

The project is deliberately intended to connect several abstraction levels:

```text
business question
    ↓
data-processing algorithm
    ↓
application concurrency and resource model
    ↓
JVM allocation, heap and garbage collection
    ↓
Java I/O and native calls
    ↓
OS page cache, read-ahead and filesystem
    ↓
physical or virtual storage
```

The objective is not to memorize every low-level mechanism. The objective is to recognize when a lower layer matters, observe it, explain its effect on the layer above, and make changes supported by measurements.

## 2. Primary Problem

Given a collection of files whose combined size is much larger than RAM, calculate a summary such as:

- total record count;
- valid and invalid record counts;
- minimum and maximum event timestamps;
- counts by a bounded set of event types;
- counts by a bounded set of locations;
- input bytes and records processed per second.

The correct baseline solution is a sequential scan with bounded state:

```text
open file
    ↓
read next record
    ↓
validate and decode
    ↓
update bounded summary
    ↓
discard record
    ↓
repeat
```

Memory consumption should depend on the processing state, not on total input size:

```text
desired:   O(number of bounded aggregation keys)
not:       O(number of input records)
```

Processing 10 GB, 100 GB or 1 TB should not require a proportionally larger Java heap when the requested aggregation remains bounded.

## 3. Scenario and Domain Model

The initial data represents events produced by independent logistics or IoT devices. The domain story exists to provide realistic data characteristics; building a complete logistics platform is not the goal.

A location contains multiple independent devices, for example scanners, sorting equipment, sensors and gateways. Events collected during some time interval can be stored as large backlog segments.

Important properties of the data:

- timestamps are not unique;
- events from different devices are independent;
- multiple events may have the same timestamp;
- device clocks may have different offsets;
- event time may move backwards in file order;
- file order is not necessarily global business order;
- `eventId`, not timestamp, identifies an event;
- a device sequence may provide ordering only within one device;
- duplicate, malformed, delayed and out-of-order records are normal input conditions.

An example logical event contains:

```text
eventId
deviceId
occurredAt
gatewayReceivedAt
deviceSequence (optional)
parcelId
eventType
locationId
```

The first experiments do not need all possible failure cases. New data characteristics should be introduced deliberately as separate workload profiles.

## 4. Initial Storage Format

The primary large-file format is Apache Avro Object Container File (OCF).

Reasons:

- it supports sequential record processing;
- the schema is stored with the file;
- records are organized into blocks with synchronization markers;
- block compression can be introduced as a controlled experiment;
- it avoids inventing a custom container around individual Protobuf messages;
- it is suitable for generated Java records and for generic-record experiments.

Initial configuration:

```text
format:             Avro OCF
record API:         generated SpecificRecord
compression:        none
file count:         configurable
target file size:   approximately 10 GB
```

Compression, GenericRecord, record reuse, alternate block sizes, CSV and Protobuf are later comparisons. They must not be introduced simultaneously in the baseline.

The format is not a permanent architectural commitment. It is the first controlled implementation.

## 5. Deterministic Data Generator

The generator must create arbitrarily large inputs without retaining generated events in memory.

For the same configuration it must produce logically identical data. The configuration includes at least:

- seed;
- event count or target data size;
- start and end time;
- number and types of devices;
- number of locations;
- timestamp resolution;
- clock-skew model;
- event-type distribution;
- duplicate and invalid-record rates when enabled.

The generator must not use implicit nondeterminism such as:

- `Instant.now()`;
- `UUID.randomUUID()`;
- default timezone;
- default locale;
- an unspecified random seed;
- concurrency whose scheduling changes output order.

Determinism does not mean that timestamps are unique or monotonically increasing. It means that the same configuration produces the same sequence of deliberately non-ideal events.

The generator should report:

- generated records;
- generated bytes;
- duration;
- records per second;
- bytes per second;
- output file names and checksums.

## 6. Bounded State Is a Requirement, Not an Assumption

Streaming input prevents retention of the complete input, but it does not guarantee bounded memory.

These aggregations are naturally bounded when their key domains are bounded:

- total count;
- min/max timestamp;
- count by event type;
- count by a small configured set of locations.

These operations may still exhaust memory:

- exact distinct `eventId` or `parcelId` count using a `Set`;
- `GROUP BY parcelId` for hundreds of millions of parcels;
- retaining validation failures;
- collecting every device statistic when device cardinality is uncontrolled;
- sorting all events in memory.

High-cardinality processing is a later project stage and may require:

- hash partitioning and spill to disk;
- external merge sort;
- an embedded or external database;
- bounded top-K algorithms;
- probabilistic structures such as HyperLogLog or Count-Min Sketch.

Every aggregation must state whether its result is exact or approximate and what bounds its memory consumption.

## 7. Baseline Execution Model

The first correct implementation is intentionally simple:

```text
one process
one reader
one file at a time
one record at a time
one bounded summary
no application-level parallelism
```

It should run with a deliberately restricted heap, for example:

```text
-Xms512m -Xmx512m
```

Do not begin with reactive programming, virtual threads, memory mapping, object pooling, custom binary parsing or parallel file scans. These may be useful later, but only after the baseline exposes a measured limitation.

## 8. Experiment Progression

### Stage 1 — Generate large deterministic files

Generate Avro files incrementally and verify that generator heap usage remains stable as output size grows.

Questions:

- Does the generator retain any record history accidentally?
- What limits generation throughput: encoding, CPU, filesystem or storage?
- How much data is buffered by the application and the operating system?

### Stage 2 — Deliberate full materialization

Use smaller inputs and intentionally build an in-memory list of decoded events.

Questions:

- How does serialized size relate to the Java object graph size?
- What is peak heap while transforming one complete list into another?
- What happens to allocation rate and GC activity before failure?
- At what input size does the configured heap become insufficient?

This is a controlled failure experiment, not the production design.

### Stage 3 — Sequential streaming summary

Scan all files while retaining only a bounded accumulator.

Success condition:

```text
100 GB processed with a 512 MB maximum heap and stable live-set size
```

Questions:

- Is live heap independent of processed input size?
- What allocations occur per record?
- Is the application CPU-bound, allocation-bound or I/O-bound?
- Does throughput remain stable over time?

### Stage 4 — Buffer and Avro block experiments

Change one buffering or block parameter at a time.

Observe:

- throughput;
- system-call count and size;
- allocation rate;
- CPU usage;
- page-cache behavior;
- diminishing returns from larger buffers.

Keep these concepts separate:

```text
record processing
application buffering
Avro block size
OS page cache
filesystem read-ahead
physical storage I/O
```

### Stage 5 — Compression

Compare no compression with selected Avro codecs.

Expected trade-off:

```text
less storage and fewer input bytes
                versus
more decompression CPU
```

Determine whether the workload is constrained by storage bandwidth or CPU before interpreting results.

### Stage 6 — Allocation reduction

Use JFR or another profiler to identify allocations from:

- Avro records;
- `String` and `Utf8` conversion;
- `byte[]` buffers;
- timestamp conversion;
- validation errors;
- maps and temporary transformation objects.

Then compare, separately:

- `SpecificRecord` and `GenericRecord`;
- allocating a new record and reusing a record instance;
- transforming into a domain object and aggregating directly from the input record.

Clarity remains a constraint. Removing a small allocation is not automatically an improvement.

### Stage 7 — Parallel file processing

Process several independent files concurrently with an explicit concurrency limit.

Questions:

- Does concurrency increase throughput?
- At what point is storage bandwidth saturated?
- Does decompression make the workload CPU-bound?
- Does concurrency cause more page-cache churn?
- How does it affect allocation rate, GC and tail latency?

Do not assume that 10 files imply 10 useful reader threads.

### Stage 8 — High-cardinality aggregation and spill

Introduce an aggregation whose state no longer fits in heap. Observe the failure, then implement a disk-backed strategy.

This stage introduces external-memory algorithms deliberately, after bounded streaming is understood.

### Stage 9 — Failure and recovery

Terminate processing after part of the input has been scanned.

Compare:

- restart from the beginning;
- completed-file checkpoints;
- Avro-block checkpoints;
- deliberately overlapping checkpoints;
- corrupted or truncated input segments.

Recovery must define duplicate-processing semantics explicitly.

### Stage 10 — Backpressure and downstream processing

Only after the single-stage scanner is understood, separate reading from processing:

```text
reader → bounded queue → workers → sink
```

Make the sink deliberately slower than the reader and observe producer blocking, queue depth, memory stability and saturation.

### Stage 11 — Live workload versus backlog

If the logistics scenario is extended into a service, run live traffic while a large backlog is processed.

The objective is not maximum backlog throughput. It is maximum safe backlog throughput while preserving the service level of live work.

## 9. Observation Layers

### Application-level observations

Record at least:

- files completed and active file;
- bytes and records processed;
- records/s and MB/s;
- invalid records;
- progress and estimated remaining work;
- aggregation-state cardinality;
- queue depth and worker activity when concurrency is introduced.

### JVM observations

Use Java Flight Recorder as the primary JVM investigation tool.

Observe:

- heap used and committed;
- live-set size after GC;
- allocation rate;
- allocation hot spots and object types;
- young and old GC frequency;
- GC pause time;
- thread states and contention;
- CPU samples;
- file read activity when available;
- safepoints;
- native memory when relevant.

Supporting JVM tools may include:

```text
jcmd
jstat
jmap, only when its cost is understood
Native Memory Tracking
async-profiler
```

The project must preserve the distinction between:

```text
total allocation volume
live heap
retained heap
RSS
native memory
page cache
```

### OS and process observations

On Linux, useful tools may include:

```text
pidstat
vmstat
iostat
sar
time
/proc/<pid>/status
/proc/<pid>/io
```

Observe:

- process CPU usage, including user and system time;
- RSS and major/minor page faults;
- bytes requested by the application versus bytes read by the OS;
- block-device throughput, utilization and queueing;
- context switches;
- read-ahead and page-cache effects;
- warm-cache versus cold-cache runs.

Dropping OS caches affects the entire host and requires care. It must not be done casually or on a shared environment.

### System-call observations

Use `strace`, `perf trace`, `perf` or equivalent tools when a specific hypothesis requires syscall-level evidence.

Questions may include:

- How many `read` or `pread64` calls occur?
- What are their requested and returned sizes?
- Is the application making many tiny reads?
- How much time is spent in syscalls?
- Are files accessed sequentially?
- Does application buffering materially change syscall behavior?

Syscall tracing has overhead. Do not treat a heavily traced run as a clean throughput benchmark.

### Storage observations

Identify the storage environment before comparing results:

- local SSD or HDD;
- network-attached storage;
- virtualized or container-mounted filesystem;
- encrypted filesystem;
- cloud volume;
- warm or cold OS cache.

A throughput number without the storage and cache context is not a reusable result.

## 10. Experimental Method

Performance work follows this loop:

```text
hypothesis
    ↓
measurement design
    ↓
baseline measurement
    ↓
explanation
    ↓
one controlled change
    ↓
repeat measurement
    ↓
accept, reject or refine hypothesis
```

Each experiment should record:

```text
Question:
Hypothesis:
Changed variable:
Controlled variables:
Input data and checksum:
JDK and JVM options:
Hardware / VM / container:
Storage and filesystem:
Cache state:
Warm-up method:
Measurement tools:
Results:
Interpretation:
Possible confounders:
Next experiment:
```

Do not compare measurements when several important variables changed at once.

## 11. Core Metrics

Depending on the experiment, collect:

- records/s;
- input MB/s;
- wall-clock duration;
- user CPU and system CPU;
- CPU utilization per core and process;
- allocation bytes/s;
- live heap after GC;
- peak heap;
- GC frequency and pause time;
- RSS and native memory;
- page faults;
- syscall count and average read size;
- storage throughput, utilization and latency;
- queue depth;
- aggregation-state cardinality;
- restart and recovery time.

Throughput alone is insufficient. A change that increases throughput may also increase memory, CPU cost, recovery cost or operational complexity.

## 12. Important Distinctions

Do not collapse the following concepts:

```text
file size                         != Java object-graph size
allocation rate                  != live heap
heap                             != total process memory
RSS                              != heap
application read                 != physical disk read
application buffer               != OS page cache
buffering                        != batching
parallelism                      != higher throughput
compression ratio                != end-to-end performance
event timestamp                  != event identity
file order                       != global business order
streaming input                  != bounded aggregation state
successful file transfer         != successful business processing
```

## 13. Non-goals

The project should not become, unless a later experiment explicitly requires it:

- a complete logistics platform;
- a database engine;
- a distributed filesystem;
- a custom serialization framework;
- a Kafka replacement;
- an exercise in using every Java concurrency API;
- a collection of premature low-level optimizations;
- a benchmark whose only result is the largest possible throughput number.

The project may explore lower-level mechanisms, but only when they help explain an observed behavior.

## 14. Engineering Principles

1. Begin with the simplest correct implementation.
2. Keep resources bounded unless unbounded behavior is the experiment.
3. Introduce one important variable at a time.
4. Keep input reproducible and preserve checksums.
5. State the expected resource bottleneck before optimizing.
6. Measure both improvements and regressions.
7. Treat crashes, partial progress, malformed data and saturation as normal conditions.
8. Prefer an explainable result over an impressive but unrepeatable number.
9. Preserve code clarity unless evidence justifies additional complexity.
10. Separate observations from explanations and explanations from conclusions.

## 15. Guidance for AI Assistance

AI working on this repository must optimize for engineering learning, not for producing the most sophisticated implementation immediately.

Before proposing a change, AI should state:

```text
What resource might be limiting the system?
What evidence currently supports that hypothesis?
What single change is proposed?
Which metric should change?
What might become worse?
How will the result be verified?
```

AI must not introduce several of these at the same time without an explicit request:

- custom parsing;
- memory mapping;
- reactive programming;
- virtual threads;
- object pooling;
- parallel file reads;
- compression changes;
- record reuse;
- custom native code;
- off-heap storage.

AI should challenge requests that accidentally make memory proportional to input size or unbounded key cardinality.

AI should preserve deliberately naive implementations when they exist as experiment baselines. Optimized implementations should normally be added as separate strategies rather than erasing the baseline.

AI should not interpret every profiler event as a problem. It should distinguish observed cost from a demonstrated bottleneck.

When results are surprising, AI should first look for confounders such as:

- OS cache state;
- JIT warm-up;
- different input bytes;
- different compression ratio;
- background host activity;
- storage throttling;
- profiler or tracer overhead;
- container CPU and memory limits;
- accidental retention of records;
- growing aggregation cardinality.

## 16. Initial Milestone

The first milestone is complete when the repository can:

1. Generate deterministic Avro OCF files incrementally.
2. Generate multiple large files without generator heap growth proportional to output size.
3. Sequentially scan the generated files.
4. Produce a bounded summary.
5. Run with `-Xms512m -Xmx512m`.
6. Report records, bytes, duration, records/s and MB/s.
7. Produce a JFR recording for the scan.
8. Demonstrate stable live heap while processed bytes continue to grow.
9. Reproduce the same logical summary from the same generator configuration.

The milestone target is eventually:

```text
100 GB input processed successfully with a 512 MB maximum heap
```

Smaller files should be used while developing correctness. Very large files are generated only for experiments that need them.

## 17. Suggested Initial Components

```text
generator
├── GeneratorConfig
├── DeviceEventGenerator
├── AvroSegmentWriter
└── GenerationReport

scanner
├── AvroEventReader
├── EventValidator
├── SummaryAccumulator
├── FileSummary
└── ScanReport

cli
├── generate
└── summarize

experiments
├── experiment definition
├── run configuration
├── captured metrics
└── written interpretation
```

The package structure is provisional. It should remain small until real responsibilities emerge.

## 18. Definition of Project Success

Success is not merely that the application processes 100 GB.

Success means being able to explain, with evidence:

- why the data does not need to fit in RAM;
- what state does need to fit in RAM;
- which objects are allocated and retained;
- what GC is doing and why;
- how Java read operations become syscalls;
- how the OS page cache and read-ahead affect measurements;
- whether the current limit is CPU, allocation, syscall overhead, storage or aggregation state;
- how concurrency changes the bottleneck;
- how the process recovers after partial progress;
- why a proposed optimization helps and what it costs.

The final skill being developed is the ability to move confidently between application design, JVM behavior and operating-system behavior without confusing the responsibilities of those layers.
