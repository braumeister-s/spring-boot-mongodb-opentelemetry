export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8080;
export OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true
export OTEL_SERVICE_NAME=spring-boot-mongo
export OTEL_RESOURCE_ATTRIBUTES=deployment.environment=development,service.namespace=mongo-demo,service.instance.id=0


export OTEL_METRIC_EXPORT_INTERVAL=30000
# Verzögerung für Traces (Spans) - Batch Span Processor Konfiguration
export OTEL_BSP_SCHEDULE_DELAY=30000
# Verzögerung für Logs - Batch Log Record Processor Konfiguration
export OTEL_BLRP_SCHEDULE_DELAY=30000


# Aktiviere nur Metriken
export OTEL_METRICS_EXPORTER=otlp
export OTEL_LOGS_EXPORTER=none
export OTEL_TRACES_EXPORTER=none

mvn clean package -DargLine="-javaagent:./otel/agent/opentelemetry-javaagent.jar"

#mvn clean package -DskipTests -DargLine="-javaagent:./otel/agent/opentelemetry-javaagent.jar"
#java -javaagent:./otel/agent/opentelemetry-javaagent.jar -jar target/spring-boot-mongo-0.0.1-SNAPSHOT.jar