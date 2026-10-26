package ip.logistics.shipping.edgeingestion.service;

import java.lang.ScopedValue;

public class IngestionServiceApplication {
    private static final ScopedValue<String> VALUE = ScopedValue.newInstance();

    void main() {
        System.out.println("Starting ingestion service...");

        ScopedValue.where(VALUE, "Works on JDK 25+")
                .run(() -> System.out.println(VALUE.get()));


        System.out.println("Stopping ingestion service");
    }
}
