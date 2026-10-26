package ip.logistics.shipping.edgeingestion.generator;

import ip.logistics.shipping.edgeingestion.avro.DeviceEvent;
import ip.logistics.shipping.edgeingestion.avro.EventType;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AvroFileGenerator {

    private static final EventType[] EVENT_TYPES = EventType.values();
    private static final Instant BASE_TIME = Instant.parse("2026-09-22T12:27:29.265Z");

    private AvroFileGenerator() {
    }

    static void main(String[] args) throws IOException {
        Path output = Path.of(
                args.length > 0
                        ? args[0]
                        : "build/data/device-events.avro"
        );

        int eventCount = args.length > 1
                ? Integer.parseInt(args[1].replace("_", ""))
                : 1_000;

        Path parentDirectory = output.getParent();

        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }

        var datumWriter = new SpecificDatumWriter<DeviceEvent>(
                DeviceEvent.class
        );

        try (var fileWriter = new DataFileWriter<>(datumWriter)) {
            fileWriter.create(
                    DeviceEvent.getClassSchema(),
                    output.toFile()
            );

            for (int index = 0; index < eventCount; index++) {
                fileWriter.append(createEvent());
            }
        }

        System.out.printf(
                "Generated %,d events in %s%n",
                eventCount,
                output.toAbsolutePath()
        );
    }

    private static DeviceEvent createEvent() {
        var random = ThreadLocalRandom.current();
        var simulatedDeviceCount = 100;
        var simulatedOccurredAtCount = 60;
        return DeviceEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setDeviceId("device-" + random.nextInt(simulatedDeviceCount))
                .setOccurredAt(BASE_TIME.minusSeconds(random.nextInt(simulatedOccurredAtCount)))
                .setEventType(EVENT_TYPES[random.nextInt(EVENT_TYPES.length)])
                .build();
    }
}