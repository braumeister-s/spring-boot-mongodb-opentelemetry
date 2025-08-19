package com.example.mongo.otel;

import io.opentelemetry.proto.metrics.v1.MetricsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1")
@Slf4j
public class OtlpReceiver {
    private final Set<String> metricNames = Collections.synchronizedSet(new HashSet<>());
    // Map: MetricName -> Map<AttributeKey, AttributeValue>
    private final Map<String, Map<String, String>> metricResourceAttributes = Collections.synchronizedMap(new HashMap<>());

    // Statische Variable für Anwendungsstart-Zeit
    private static final long applicationStartTime = System.currentTimeMillis();

    // Flag um zu prüfen, ob es der erste Aufruf ist
    private volatile boolean firstMetricsCallReceived = false;

    @PostMapping(path = "/metrics", consumes = "application/x-protobuf")
    public ResponseEntity<Void> receiveMetrics(@RequestBody byte[] body) {
        // Messe Zeit beim ersten Aufruf
        if (!firstMetricsCallReceived) {
            long currentTime = System.currentTimeMillis();
            long durationMs = currentTime - applicationStartTime;
            double durationSeconds = durationMs / 1000.0;

            log.info("Erste Metriken {} ms ({} Sekunden) nach Anwendungsstart empfangen",
                    durationMs, String.format("%.3f", durationSeconds));

            firstMetricsCallReceived = true;
        }

        try {
            MetricsData metricsData = MetricsData.parseFrom(body);

            // Verarbeite jede Resource-Attribute für diese ResourceMetrics
            metricsData.getResourceMetricsList().forEach(resourceMetric -> {
                // Extrahiere Ressource-Attribute für diese ResourceMetrics
                Map<String, String> currentResourceAttributes = new HashMap<>();
                if (resourceMetric.hasResource()) {
                    resourceMetric.getResource().getAttributesList().forEach(attribute -> {
                        String key = attribute.getKey();
                        String value = extractAttributeValue(attribute);
                        currentResourceAttributes.put(key, value);
                        log.debug("Resource Attribute: {} = {}", key, value);
                    });
                }

                // Verarbeite alle Metriken in dieser ResourceMetrics
                resourceMetric.getScopeMetricsList().forEach(scopeMetric -> {
                    scopeMetric.getMetricsList().forEach(metric -> {
                        String metricName = metric.getName();
                        metricNames.add(metricName);

                        // Speichere Ressource-Attribute pro Metrik
                        metricResourceAttributes.put(metricName, new HashMap<>(currentResourceAttributes));

                        log.debug("Metrik empfangen: {}", metricName);
                        currentResourceAttributes.forEach((k, v) -> log.debug("  Attribut: {} = {}", k, v));
                    });
                });
            });

        } catch (Exception e) {
            log.error("Failed to parse MetricsData", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        log.info("{} unterschiedliche Metriken mit Ressource-Attributen wurden eingeliefert!",
                metricNames.size());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String extractAttributeValue(io.opentelemetry.proto.common.v1.KeyValue attribute) {
        return attribute.getValue().toString();
    }

    @PostMapping(path = "/traces", consumes = "application/x-protobuf")
    public ResponseEntity<Void> receiveTraces(@RequestBody byte[] body) {
//        try {
//            TracesData tracesData = TracesData.parseFrom(body);
//            logger.info("Received Trace: {}", tracesData);
//        } catch (Exception e) {
//            logger.error("Failed to parse TracesData", e);
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(path = "/logs", consumes = "application/x-protobuf")
    public ResponseEntity<Void> receiveLogs(@RequestBody byte[] body) {
//        try {
//            LogsData logsData = LogsData.parseFrom(body);
//            logger.info("Received Log: {}", logsData);
//        } catch (Exception e) {
//            logger.error("Failed to parse LogsData", e);
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(path = "/metrics/names")
    public ResponseEntity<Set<String>> getMetricNames() {
        return ResponseEntity.ok(metricNames);
    }

    @GetMapping(path = "/metrics/received")
    public ResponseEntity<Boolean> hasReceivedMetrics() {
        return ResponseEntity.ok(!metricNames.isEmpty());
    }

    @GetMapping(path = "/metrics/received/names")
    public ResponseEntity<Set<String>> getReceivedMetricNames() {
        return ResponseEntity.ok(metricNames);
    }

    @GetMapping(path = "/metrics/resource/attributes")
    public ResponseEntity<Map<String, Map<String, String>>> getMetricResourceAttributes() {
        return ResponseEntity.ok(metricResourceAttributes);
    }

    @GetMapping(path = "/metrics/{metricName}/resource/attributes")
    public ResponseEntity<Map<String, String>> getResourceAttributesForMetric(@PathVariable String metricName) {
        Map<String, String> attributes = metricResourceAttributes.get(metricName);
        if (attributes != null) {
            return ResponseEntity.ok(attributes);
        } else {
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }
}