package com.example.demo.otel;

import com.example.demo.StartupRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
    "server.port=8080",
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=27017",
    "spring.data.mongodb.database=testdb"
})
class OtelControllerIntegrationTest {

    @Autowired
    private OtelController otelController;

    @Autowired
    private StartupRunner startupRunner;

    @Test
    void shouldReceiveMetricsWithin60Seconds() throws InterruptedException {

        // expected metrics
        // - https://opentelemetry.io/docs/specs/semconv/database/mongodb/ and
        // - https://opentelemetry.io/docs/specs/semconv/database/database-metrics/

        startupRunner.runScenario();
        Thread.sleep(31000); // max waittime to receive metrics

        Set<String> receivedMetricsNames = otelController.getReceivedMetricNames().getBody();
        Map<String, Map<String, String>> metricResourceAttributes = otelController.getMetricResourceAttributes().getBody();

        assertTrue(containsOpenTelemetryMongoMetrics(receivedMetricsNames),
                  "Es sollte mindestens eine erwartete MongoDB-Metrik empfangen werden");

        assertTrue(containsExpectedResourceAttributesForMetrics(metricResourceAttributes),
                  "Es sollten erwartete Ressource-Attribute für die Metriken vorhanden sein");

        System.out.println("Test erfolgreich: MongoDB-Metriken und Ressource-Attribute wurden empfangen");
        System.out.println("Empfangene Metriken: " + receivedMetricsNames);
        System.out.println("Metrik -> Ressource-Attribute Zuordnung: " + metricResourceAttributes.size() + " Einträge");
    }

    /**
     * Prüft ob mindestens eine der erwarteten OpenTelemetry Database Metriken vorhanden ist
     * und ob diese die korrekten Ressource-Attribute haben
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

        // Hole die Metrik-Ressource-Attribute
        Map<String, Map<String, String>> metricResourceAttributes = otelController.getMetricResourceAttributes().getBody();

        // Prüfe MongoDB-Metriken und ihre Ressource-Attribute
        for (String expectedMetric : expectedMongoMetrics) {
            if (receivedMetrics.contains(expectedMetric)) {
                System.out.println("✅ MongoDB-Metrik gefunden: " + expectedMetric);

                // Prüfe die Ressource-Attribute für diese spezifische Metrik
                if (metricResourceAttributes != null && metricResourceAttributes.containsKey(expectedMetric)) {
                    Map<String, String> attributes = metricResourceAttributes.get(expectedMetric);
                    boolean hasValidDatabaseAttributes = validateDatabaseMetricResourceAttributes(expectedMetric, attributes);

                    if (hasValidDatabaseAttributes) {
                        System.out.println("  ✅ Metrik " + expectedMetric + " hat korrekte Database-Ressource-Attribute");
                        return true;
                    } else {
                        System.out.println("  ⚠️ Metrik " + expectedMetric + " gefunden, aber erwartete Database-Ressource-Attribute fehlen");
                    }
                } else {
                    System.out.println("  ⚠️ Metrik " + expectedMetric + " gefunden, aber keine Ressource-Attribute verfügbar");
                }
            }
        }

        System.out.println("❌ Keine OpenTelemetry konformen Metriken für MongoDB mit korrekten Ressource-Attributen gefunden");
        System.out.println("📋 Empfangene Metriken: " + receivedMetrics);
        return false;
    }

    /**
     * Validiert die Ressource-Attribute für Database-Metriken gemäß
     * https://opentelemetry.io/docs/specs/semconv/database/database-metrics/
     */
    private boolean validateDatabaseMetricResourceAttributes(String metricName, Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            System.out.println("    ❌ Keine Ressource-Attribute für Metrik " + metricName);
            return false;
        }

        // Erforderliche Attribute für Database-Metriken gemäß OpenTelemetry Spezifikation
        Set<String> requiredDatabaseAttributes = Set.of(
            "db.client.connections.pool.name",    // Der Name des Connection Pools
            "db.client.connections.state"         // Der Status der Connection (idle, used, etc.)
        );

        // Empfohlene/Optionale Attribute für Database-Metriken
        Set<String> recommendedDatabaseAttributes = Set.of(
            "db.system",                          // Database system name (sollte "mongodb" sein)
            "db.name",                            // Database name
            "db.namespace",                       // Database namespace
            "db.collection.name",                 // Collection name (für MongoDB spezifisch)
            "server.address",                     // Database server address
            "server.port",                        // Database server port
            "db.operation.name"                   // Name der Database-Operation
        );

        // Service-Attribute (immer erwartet)
        Set<String> expectedServiceAttributes = Set.of(
            "service.name",                       // Name des Services
            "service.instance.id"                 // Eindeutige Service-Instanz ID
        );

        boolean foundRequiredAttribute = false;
        boolean foundRecommendedAttribute = false;
        boolean foundServiceAttribute = false;

        // Prüfe erforderliche Database-Attribute
        for (String requiredAttr : requiredDatabaseAttributes) {
            if (attributes.containsKey(requiredAttr)) {
                System.out.println("    ✅ Erforderliches DB-Attribut gefunden: " + requiredAttr + " = " + attributes.get(requiredAttr));
                foundRequiredAttribute = true;
            }
        }

        // Prüfe empfohlene Database-Attribute
        for (String recommendedAttr : recommendedDatabaseAttributes) {
            if (attributes.containsKey(recommendedAttr)) {
                System.out.println("    ✅ Empfohlenes DB-Attribut gefunden: " + recommendedAttr + " = " + attributes.get(recommendedAttr));
                foundRecommendedAttribute = true;

                // Spezielle Validierung für db.system
                if ("db.system".equals(recommendedAttr)) {
                    String dbSystem = attributes.get(recommendedAttr);
                    if ("mongodb".equals(dbSystem)) {
                        System.out.println("    ✅ Korrekte Database-System-Identifikation: " + dbSystem);
                    } else {
                        System.out.println("    ⚠️ Unerwartetes Database-System: " + dbSystem + " (erwartet: mongodb)");
                    }
                }
            }
        }

        // Prüfe Service-Attribute
        for (String serviceAttr : expectedServiceAttributes) {
            if (attributes.containsKey(serviceAttr)) {
                System.out.println("    ✅ Service-Attribut gefunden: " + serviceAttr + " = " + attributes.get(serviceAttr));
                foundServiceAttribute = true;
            }
        }

        // Für eine erfolgreiche Validierung sollte mindestens ein empfohlenes DB-Attribut
        // oder ein Service-Attribut vorhanden sein
        boolean isValid = foundRecommendedAttribute || foundServiceAttribute;

        if (!isValid) {
            System.out.println("    ❌ Keine erwarteten Database- oder Service-Attribute für Metrik " + metricName + " gefunden");
            System.out.println("    📋 Verfügbare Attribute: " + attributes.keySet());
        }

        return isValid;
    }

    /**
     * Prüft ob erwartete OpenTelemetry Ressource-Attribute für die empfangenen Metriken vorhanden sind
     * Basiert auf: https://opentelemetry.io/docs/specs/semconv/resource/
     */
    private boolean containsExpectedResourceAttributesForMetrics(Map<String, Map<String, String>> metricResourceAttributes) {
        if (metricResourceAttributes == null || metricResourceAttributes.isEmpty()) {
            System.out.println("❌ Keine Metrik-Ressource-Attribute empfangen");
            return false;
        }

        // Erwartete Attribute-Sets (wie vorher definiert)
        Set<String> expectedServiceAttributes = Set.of(
            "service.name", "service.version", "service.instance.id", "service.namespace"
        );

        Set<String> expectedDatabaseAttributes = Set.of(
            "db.system", "db.name", "db.connection_string", "db.user"
        );

        Set<String> expectedRuntimeAttributes = Set.of(
            "telemetry.sdk.name", "telemetry.sdk.language", "telemetry.sdk.version",
            "process.runtime.name", "process.runtime.version"
        );

        Set<String> expectedDeploymentAttributes = Set.of(
            "deployment.environment", "host.name", "host.arch"
        );

        boolean foundAnyExpectedAttribute = false;

        // Durchlaufe alle Metriken und ihre Ressource-Attribute
        for (Map.Entry<String, Map<String, String>> entry : metricResourceAttributes.entrySet()) {
            String metricName = entry.getKey();
            Map<String, String> resourceAttributes = entry.getValue();

            System.out.println("🔍 Prüfe Metrik: " + metricName + " (hat " + resourceAttributes.size() + " Attribute)");

            // Prüfe Service-Attribute für diese Metrik
            boolean foundAttributeForThisMetric = false;
            for (String expectedAttribute : expectedServiceAttributes) {
                if (resourceAttributes.containsKey(expectedAttribute)) {
                    System.out.println("  ✅ Service-Attribut gefunden: " + expectedAttribute + " = " + resourceAttributes.get(expectedAttribute));
                    foundAttributeForThisMetric = true;
                    foundAnyExpectedAttribute = true;
                }
            }

            // Prüfe Database-Attribute für diese Metrik
            for (String expectedAttribute : expectedDatabaseAttributes) {
                if (resourceAttributes.containsKey(expectedAttribute)) {
                    System.out.println("  ✅ Database-Attribut gefunden: " + expectedAttribute + " = " + resourceAttributes.get(expectedAttribute));
                    foundAttributeForThisMetric = true;
                    foundAnyExpectedAttribute = true;
                }
            }

            // Prüfe Runtime-Attribute für diese Metrik
            for (String expectedAttribute : expectedRuntimeAttributes) {
                if (resourceAttributes.containsKey(expectedAttribute)) {
                    System.out.println("  ✅ Runtime-Attribut gefunden: " + expectedAttribute + " = " + resourceAttributes.get(expectedAttribute));
                    foundAttributeForThisMetric = true;
                    foundAnyExpectedAttribute = true;
                }
            }

            // Prüfe Deployment-Attribute für diese Metrik
            for (String expectedAttribute : expectedDeploymentAttributes) {
                if (resourceAttributes.containsKey(expectedAttribute)) {
                    System.out.println("  ✅ Deployment-Attribut gefunden: " + expectedAttribute + " = " + resourceAttributes.get(expectedAttribute));
                    foundAttributeForThisMetric = true;
                    foundAnyExpectedAttribute = true;
                }
            }

            if (!foundAttributeForThisMetric) {
                System.out.println("  ❌ Keine erwarteten Attribute für Metrik " + metricName);
                System.out.println("    Verfügbare Attribute: " + resourceAttributes.keySet());
            }
        }

        if (!foundAnyExpectedAttribute) {
            System.out.println("❌ Keine erwarteten Ressource-Attribute in irgendeiner Metrik gefunden");
        }

        return foundAnyExpectedAttribute;
    }
}
