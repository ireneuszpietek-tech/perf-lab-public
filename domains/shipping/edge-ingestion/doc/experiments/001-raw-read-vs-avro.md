## Question
Does Avro decoding significantly limit scanning throughput?

## Hypothesis
Scanning raw bytes will be at least twice as fast as scanning Avro.
I am setting this 2× threshold as a constraint for the experiment.

## Experiment
Same file, sequential scan:
A. Read into a reusable `byte[]`, count bytes;
B. Avro decoding, count records.

I record: time, input MB/s, CPU, GC.
I note the execution details and potential impact on the page cache.

# Raw-byte read vs Avro processing

## Question
How much throughput is lost when processing Avro records compared with reading the file as bytes?

## Setup
Both runs used the same file: 10,783,540,522 bytes containing 200,000,000 records.

The raw-byte reader reused a 64 KiB buffer. The Avro reader decoded and counted records.

## Results

| Variant | Elapsed time | Input throughput |
|---|---:|---:|
| Raw-byte read | 1.100 s | 9,349.96 MiB/s |
| Avro decoding + record count | 10.594 s | ~970.85 MiB/s |

Avro processing took approximately 9.63 times longer.

## Interpretation
These runs show a substantial throughput difference between raw-byte reading and Avro processing.

The difference does not isolate decoding cost. It may also include allocation, record handling, and decompression if compression is enabled.

OS page cache state was not measured. These results do not establish physical storage throughput.

## Next step
Capture a CPU profile of the Avro run to identify where execution time is spent.