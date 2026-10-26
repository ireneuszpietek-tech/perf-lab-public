package ip.logistics.shipping.edgeingestion.experiments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * full sequential file scan
 */
public class RawByteScan {
    static void main() throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long totalBytes = 0;

        long start = System.nanoTime();

        try (var inputStream = Files.newInputStream(Path.of("build/data/device-events.avro"), StandardOpenOption.READ)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
            }
        }

        long elapsedNanos = System.nanoTime() - start;

        double seconds = elapsedNanos / 1_000_000_000.0;
        double mibPerSecond = (totalBytes / (1024.0 * 1024.0)) / seconds;

        System.out.printf(
                "Read %,d bytes in %.3f s: %.2f MiB/s%n",
                totalBytes, seconds, mibPerSecond
        );

        System.out.println("Bytes read: " + totalBytes);
        System.out.println("Bytes read: " + totalBytes/1000 + " kb");
        System.out.println("Bytes read: " + totalBytes/1000/1000 + " mb");
        System.out.println("Bytes read: " + totalBytes/1000/1000/1000 + " gb");
    }

}
