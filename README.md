# Spring Boot MongoDB OpenTelemetry Demo

## Projektbeschreibung

Dieses Projekt demonstriert die Integration von **OpenTelemetry** in eine **Spring Boot**-Anwendung mit **MongoDB**, um automatisch Metriken, Traces und Logs zu erfassen und an einen OTLP-Empfänger zu senden.

> Aktuell scheitert der Test! Das Ziel ist es, die automatische Instrumentierung von MongoDB so zu konfigurieren, dass die Metriken wie erwartet erfasst und exportiert werden. Der Test prüft, ob die erwarteten Metriken innerhalb von 60 Sekunden empfangen werden.

## TL;DR

> ./buildAndTest.sh

Voraussetzung: **Java 17**, **Maven 3.6+**, **Podman** oder **Docker** für MongoDB Container.

### Hauptziele

- **OpenTelemetry Java Agent** zur automatischen Instrumentierung einer Spring Boot Anwendung verwenden
- **MongoDB-Metriken** automatisch erfassen (Connection Pool, Database Operations, etc.)
- **OTLP (OpenTelemetry Protocol)** für den Export der Telemetrie-Daten nutzen
- **Integrationstests** zur Validierung der OpenTelemetry-Instrumentierung implementieren

## Architektur

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │   OpenTelemetry │    │   OTLP Receiver │
│   Application   │───▶│   Java Agent    │───▶│   (Internal)    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                              │
         ▼                                              │
┌─────────────────┐                                     │
│     MongoDB     │                                     │
│   (Container)   │                                     │
└─────────────────┘                                     │
                                                        │
┌─────────────────┐                                     │
│ Integration     │◀────────────────────────────────────┘
│ Tests           │
└─────────────────┘
```

## Technologie-Stack

- **Java 17**
- **Spring Boot 3.5.4**
- **Spring Data MongoDB**
- **OpenTelemetry Java Agent**
- **MongoDB** (über Podman Container)
- **JUnit 5** für Tests
- **Lombok** für Code-Generierung
- **Maven** für Build-Management

## Projektstruktur

```
├── buildAndTest.sh              # Build- und Test-Skript
├── podman-compose.yml           # MongoDB Container Definition
├── pom.xml                      # Maven Dependencies
├── otel/agent/                  # OpenTelemetry Java Agent
│   └── opentelemetry-javaagent.jar
├── src/main/java/
│   └── com/example/mongo/otel/
│       ├── DemoApplication.java         # Spring Boot Hauptklasse
│       ├── DemoController.java          # REST Controller
│       ├── DemoRepository.java          # MongoDB Repository
│       ├── MongoActionsGenerator.java   # MongoDB Operationen Generator
│       ├── Note.java                    # Entity Klasse
│       └── OtlpReceiver.java            # OTLP Endpunkt (Test-Receiver)
└── src/test/java/
    └── com/example/mongo/otel/
        └── MongoOtlpInstrumentationTest.java  # Integrationstests
```

### MongoDB Konfiguration

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/demodb
```

## API Endpunkte

### Anwendungs-Endpunkte

- `GET /` - Willkommensseite
- `GET /notes` - Alle Notizen abrufen
- `POST /notes` - Neue Notiz erstellen
- `GET /generate-actions` - MongoDB Operationen generieren

### OTLP-Receiver Endpunkte (Test)

- `POST /v1/metrics` - OTLP Metriken empfangen
- `GET /metrics/names` - Empfangene Metrik-Namen abrufen
- `GET /metrics/received` - Prüfen ob Metriken empfangen wurden
- `GET /metrics/resource/attributes` - Alle Metrik-Ressource-Attribute
- `GET /metrics/{metricName}/resource/attributes` - Ressource-Attribute für spezifische Metrik

## Tests

### Integrationstests

Die `MongoOtlpInstrumentationTest` Klasse validiert:

1. **Metriken-Empfang**: Prüft ob erwartete MongoDB-Metriken innerhalb von 60 Sekunden empfangen werden

### Test ausführen

```bash
> ./buildAndTest.sh - evtl. Anpassen, falls eine alternative MongoDB-Instanz verwendet wird.
```

## Erwartete Metriken

Das Projekt validiert folgende OpenTelemetry-konforme MongoDB-Metriken:

- `db.client.operation.duration` - Dauer von Database-Operationen
- `db.client.response.returned_rows` - Anzahl zurückgegebener Zeilen
- `db.client.connection.count` - Anzahl aktive Verbindungen
- `db.client.connection.idle.max` - Maximale Anzahl idle Verbindungen
- `db.client.connection.idle.min` - Minimale Anzahl idle Verbindungen
- `db.client.connection.max` - Maximale Anzahl Verbindungen im Pool
- `db.client.connection.pending_requests` - Anzahl wartender Verbindungsanfragen
- `db.client.connection.timeouts` - Anzahl Connection Timeouts
- `db.client.connection.create_time` - Zeit für Verbindungserstellung
- `db.client.connection.wait_time` - Wartezeit für verfügbare Verbindung
- `db.client.connection.use_time` - Nutzungszeit einer Verbindung

## Weiterführende Links

- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [Spring Boot MongoDB](https://spring.io/guides/gs/accessing-data-mongodb/)
- [OTLP Specification](https://opentelemetry.io/docs/specs/otlp/)
