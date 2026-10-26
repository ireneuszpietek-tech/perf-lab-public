root:$ /usr/lib/jvm/java-25-openjdk-amd64/bin/java -cp '/home/irek/IdeaProjects/perf-lab/domains/shipping/edge-ingestion/services/build/install/edge-ingestion-service/lib/*' ip.logistics.shipping.edgeingestion.generator.AvroFileGenerator build/data/device-events.avro 100_000_000

root:$ /usr/lib/jvm/java-1.25.0-openjdk-amd64/bin/java -cp '/home/irek/IdeaProjects/perf-lab/domains/shipping/edge-ingestion/services/build/install/edge-ingestion-service/lib/*' ip.logistics.shipping.edgeingestion.service.DeviceEventReader

rg 'device-events\.avro' avro-reads.log | head -n 30

strace -f -yy -e trace=read,pread64 -o avro-reads.log /usr/lib/jvm/java-1.25.0-openjdk-amd64/bin/java -cp '/home/irek/IdeaProjects/perf-lab/domains/shipping/edge-ingestion/services/build/install/edge-ingestion-service/lib/*' ip.logistics.shipping.edgeingestion.service.DeviceEventReader