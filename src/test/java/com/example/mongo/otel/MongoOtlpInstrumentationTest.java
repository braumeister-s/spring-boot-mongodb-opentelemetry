package com.example.mongo.otel;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
class MongoOtlpInstrumentationTest {

    @Autowired
    private OtlpReceiver otelController;

    @Test
    void shouldReceiveMongoMetricsWithin60Seconds() throws InterruptedException {

        // expected metrics
        // - https://opentelemetry.io/docs/specs/semconv/database/mongodb/ and
        // - https://opentelemetry.io/docs/specs/semconv/database/database-metrics/
        log.info("Warte auf den Empfang von OpenTelemetry Metriken...");
        Thread.sleep(61000); // max waittime to receive metrics

        Set<String> receivedMetricsNames = otelController.getReceivedMetricNames().getBody();

        assertTrue(containsOpenTelemetryMongoMetrics(receivedMetricsNames),
                  "Es sollte mindestens eine erwartete MongoDB-Metrik empfangen werden");

        log.info("Test erfolgreich: MongoDB-Metriken wurden empfangen");
        logReceivedMetrics(receivedMetricsNames);
    }

    /**
     * Prüft ob mindestens eine der erwarteten OpenTelemetry Database Metriken vorhanden ist
     * Basiert auf: https://opentelemetry.io/docs/specs/semconv/database/mongodb/
     * und: https://opentelemetry.io/docs/specs/semconv/database/database-metrics/
     */
    private boolean containsOpenTelemetryMongoMetrics(Set<String> receivedMetrics) {
        if (receivedMetrics == null || receivedMetrics.isEmpty()) {
            return false;
        }

        // MongoDB-spezifische Metriken
        Set<String> expectedMongoMetrics = Set.of(
            "db.client.operation.duration",          // Duration of database client operations
            "db.client.response.returned_rows",      // Number of rows returned by database operations
            "db.client.connection.count",            // Number of database connections currently in use
            "db.client.connection.idle.max",         // Maximum number of idle connections in the pool
            "db.client.connection.idle.min",         // Minimum number of idle connections in the pool
            "db.client.connection.max",              // Maximum number of connections allowed in the pool
            "db.client.connection.pending_requests", // Number of pending requests for database connections
            "db.client.connection.timeouts",         // Number of connection timeouts that occurred
            "db.client.connection.create_time",      // Time it took to create a new database connection
            "db.client.connection.wait_time",        // Time spent waiting for an available connection
            "db.client.connection.use_time"          // Time spent using a database connection
        );

        // Prüfe MongoDB-Metriken
        for (String expectedMetric : expectedMongoMetrics) {
            if (receivedMetrics.contains(expectedMetric)) {
                log.info("✅ MongoDB-Metrik gefunden: {}", expectedMetric);
                return true;
            }
        }

        log.info("❌ Keine OpenTelemetry konformen Metriken für MongoDB gefunden");
        logReceivedMetrics(receivedMetrics);
        return false;
    }

    private static void logReceivedMetrics(Set<String> receivedMetrics) {
        log.info("📋 Empfangene Metriken: ");
        receivedMetrics.stream().sorted().forEach(m -> log.info(" - {}", m));
    }
}
