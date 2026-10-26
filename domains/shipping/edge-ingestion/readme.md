Edge / IoT Event Ingestion

Logistics infrastructure contains devices that may operate with intermittent connectivity:

parcel scanners
sorting equipment
warehouse sensors
vehicle-mounted devices
hub gateways

Normally, events may arrive continuously.

During connectivity loss, an edge system stores events locally. When connectivity is restored, the accumulated events can be uploaded as a large backlog file.

Example:

hub-17-2026-08-17-events.csv

The file may contain millions of events and range from hundreds of MB to many GB.

timestamp,deviceId,eventId,parcelId,eventType,location
2026-08-17T08:31:12Z,scanner-17,e-10001,P123,SCANNED,HUB-17
2026-08-17T08:31:13Z,belt-04,e-10002,P124,SORTED,HUB-17

This creates two workloads that must coexist:

                         ┌── live events ───────────────┐
Edge / IoT devices ──────┤                              │
│                              ▼
│                        Logistics Core
│                              ▲
└── backlog file               │
│                    │
▼                    │
File Ingestion               │
│                    │
bounded pipeline ────────────┘

The central engineering question is not simply:

How fast can a large file be read?

It is:

How can millions of delayed events be ingested without destabilizing normal system traffic?

Ingestion Pipeline

A baseline implementation can look like:

large file
│
▼
buffered input
│
▼
record parsing
│
▼
validation
│
▼
input event
│
▼
domain transformation
│
▼
bounded queue
│
▼
workers
│
├── deduplication
├── ordering rules
├── business processing
└── persistence / event publication

The file should normally be processed as a stream, not materialized entirely in memory.

Logical processing may happen record-by-record while physical I/O remains buffered.

This distinction is important:

disk / filesystem
↓
OS page cache
↓
Java I/O buffer
↓
record
↓
domain processing
Experiments

The same workload should be implementable in progressively different ways.

1. Full materialization

Read the complete file and build an in-memory model.

file
↓
List<InputEvent>
↓
List<DomainEvent>

Questions:

How does file size relate to heap usage?
What is peak heap during transformation?
How much memory is occupied by the actual data versus JVM object representation?
At what point does the process fail?
What does GC behavior look like before failure?

This experiment demonstrates:

serialized size != JVM object graph size

and:

final heap size != peak heap during transformation
2. Streaming

Process records without retaining the complete input.

read
↓
parse
↓
transform
↓
process
↓
discard
↓
next record

Measure:

heap usage
allocation rate
GC activity
throughput
CPU utilization

A large total allocation volume is not necessarily a problem if the live set remains small.

3. Buffering

Compare different input buffering strategies and sizes.

For example:

1 KiB
8 KiB
64 KiB
256 KiB
1 MiB

The goal is not to find a universally correct buffer size.

The goal is to observe where larger buffers stop producing meaningful improvements and understand the distinction between:

logical record processing
application buffering
operating-system caching
physical storage I/O
4. Object Allocation

Use JFR or another profiler to inspect allocations produced during ingestion.

Possible allocation sources:

String
byte[]
InputEvent
DomainEvent
BigDecimal
collections
parser objects
temporary transformation objects

Questions:

How many objects are allocated per event?
How much memory is allocated per second?
Which objects survive GC?
Does an intermediate input model materially affect performance?
Can a transformation be simplified without harming code clarity?

Optimization should be driven by measurements rather than assumptions.

5. Batching

Downstream operations may be more efficient when performed in batches.

Compare:

1 event
10 events
100 events
1,000 events
10,000 events

Possible downstream operations:

database writes
event publication
deduplication lookups
remote calls

Observe the trade-off between:

throughput
latency
memory
failure granularity
retry cost

Large batches are not automatically better.

6. Backpressure

Separate file reading from event processing using a bounded queue.

                    ┌─ worker
reader ── queue ────├─ worker
├─ worker
└─ worker

Compare:

unbounded queue

with:

bounded queue

Then deliberately make consumers slower than the producer.

Observe:

queue growth
memory growth
producer blocking
throughput
latency
system behavior under saturation

The important property is:

A slow downstream component must eventually slow the producer instead of converting the speed difference into unlimited memory consumption.

7. Live Traffic vs Backlog

Run normal live traffic while importing a large backlog.

Example:

live traffic:       2,000 events/s
backlog:            5,000,000 events

Measure whether backlog processing affects:

live-event latency
p95 / p99 latency
CPU saturation
DB connection pool utilization
queue depth
error rate

Possible controls:

dedicated worker pools
concurrency limits
rate limiting
admission control
workload priorities
resource partitioning

The objective is not maximum backlog throughput.

The objective is maximum safe backlog throughput while preserving the service level of live traffic.

Failure and Recovery

Large-file ingestion must tolerate process failure.

Suppose:

5 GB file
3.7 GB processed
process crashes

The simplest recovery strategy is to restart from the beginning.

That naturally introduces duplicate events.

Because events have stable identifiers, normal deduplication mechanisms should make reprocessing safe.

A later implementation may introduce checkpoints:

fileId
byteOffset
processedEventId
checkpointTimestamp

Example:

fileId: hub-17-2026-08-17
offset: 3847192332

Checkpointing should deliberately allow a small overlap.

For example:

processed:       1,000,000
checkpoint:        990,000
CRASH
restart:           990,000

The last 10,000 events are processed again.

This gives the system at-least-once processing, making idempotency and deduplication part of the recovery model rather than exceptional behavior.

Performance Investigation

Performance work in this project should follow:

hypothesis
↓
measurement
↓
explanation
↓
change
↓
measurement

Not:

optimization idea
↓
more complicated code

Useful metrics include:

events/s
bytes/s
CPU utilization
allocation rate
live heap
GC frequency and pause time
queue depth
active workers
DB pool utilization
p50 / p95 / p99 latency
retries
duplicates
backlog age

JFR should be one of the primary JVM investigation tools.

Engineering Direction

This project should remain primarily a software/system engineering laboratory, not become a database engine or operating-system project.

Topics directly relevant to the project include:

JVM memory behavior
GC
allocation
streaming
bounded concurrency
backpressure
queues
batching
failure semantics
idempotency
retries
ordering
saturation
tail latency
resource isolation
observability
capacity

Lower-level topics may be explored when a concrete experiment motivates them:

buffer sizes
byte-oriented parsing
memory mapping
filesystem behavior
storage access patterns
custom binary formats

They should not become goals by themselves.

The preferred progression is:

simple correct implementation
↓
realistic load
↓
observe bottleneck / failure
↓
understand why
↓
change architecture or implementation
↓
measure again
Notes for AI Assistance

When helping with this repository, optimize for engineering learning rather than producing the most sophisticated implementation immediately.

Principles
Start from the simplest implementation that exposes the behavior being investigated.
Do not introduce low-level optimizations without a concrete reason or measurement.
Prefer experiments that answer one clear question.

For example:

Does streaming materially reduce live heap?

is better than simultaneously introducing:

streaming + mmap + custom parser + object pooling + virtual threads
Keep separate concepts separate.

Examples:

buffering != batching

allocation rate != live heap

file size != heap size

timeout != operation failure

throughput != latency

backlog throughput != system throughput
When proposing an optimization, state:
What resource are we optimizing?
Why might this help?
What metric should change?
What could become worse?
How do we verify it?
Prefer JVM and system measurements over theoretical micro-optimization.
Preserve realistic failure behavior. Do not make experiments artificially reliable merely to simplify implementation.
Keep resources bounded unless an experiment specifically studies unbounded behavior.
Treat duplicate delivery, retries, partial progress, crashes and saturation as normal operating conditions.
Keep the system understandable. Performance improvements that destroy the clarity of the model require strong evidence.
Long-Term Goal

The project should make it possible to move naturally between abstraction levels:

business operation
↓
application architecture
↓
concurrency and resource model
↓
JVM runtime behavior
↓
OS / storage behavior when relevant

The objective is not to know every low-level mechanism.

The objective is to understand when a lower layer matters, how to observe it, and how its behavior affects the system above it.