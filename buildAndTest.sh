
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:8080;
# Unterdrücken von Timeout-Exceptions durch höhere Timeouts
export OTEL_EXPORTER_OTLP_TIMEOUT=90000
export OTEL_METRIC_EXPORT_INTERVAL=30000

export OTEL_SERVICE_NAME=spring-boot-mongo
export OTEL_RESOURCE_ATTRIBUTES=deployment.environment=development,service.namespace=mongo-demo,service.instance.id=0

#export OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true

# Aktiviere nur Metriken
export OTEL_METRICS_EXPORTER=otlp
export OTEL_LOGS_EXPORTER=none
export OTEL_TRACES_EXPORTER=none

podman-compose up -d

mvn clean package -DargLine="-javaagent:./otel/agent/opentelemetry-javaagent.jar"

podman-compose down

#mvn clean package -DskipTests -DargLine="-javaagent:./otel/agent/opentelemetry-javaagent.jar"
#java -javaagent:./otel/agent/opentelemetry-javaagent.jar -jar target/spring-boot-mongo-0.0.1-SNAPSHOT.jar