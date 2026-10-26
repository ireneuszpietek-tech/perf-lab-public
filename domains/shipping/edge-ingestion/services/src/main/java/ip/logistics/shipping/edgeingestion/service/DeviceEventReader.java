package ip.logistics.shipping.edgeingestion.service;

import ip.logistics.shipping.edgeingestion.avro.DeviceEvent;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;
import java.nio.file.Path;

public class DeviceEventReader {


    class C1{
        long a;
        C1(long a){
            this.a = a;
        }

    }

    void main(String[] args) throws IOException {
        Path path = Path.of(args.length > 0
                ? args[0]
                : "build/data/device-events.avro");

        long start = System.nanoTime();
        long count = 0;

        try (var reader = new DataFileReader<>(
                path.toFile(),
                new SpecificDatumReader<>(DeviceEvent.class))) {

            while (reader.hasNext()) {
                DeviceEvent event = reader.next();

                ++count;
//                if (count % 1_000_000 == 0) {
//                    System.out.printf(
//                            "%d M | eventId=%s | deviceId=%s | occurredAt=%s | eventType=%s%n",
//                            count / 1_000_000,
//                            event.getEventId(),
//                            event.getDeviceId(),
//                            event.getOccurredAt(),
//                            event.getEventType()
//                    );
//                }
            }
        }

        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        System.out.printf(
                "Processed %,d records in %.3f s (%,.0f records/s)%n",
                count, seconds, count / seconds
        );
    }
}