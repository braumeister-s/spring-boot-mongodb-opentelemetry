package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.data.mongodb.observability.MongoHandlerContext;
import org.springframework.data.mongodb.observability.MongoHandlerObservationConvention;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import com.mongodb.ConnectionString;

@Configuration
class MongoObservabilityConfig {

    // 1) Eigene Convention (Beispiel mit OTel-ähnlichen Keys)
    @Bean
    MongoHandlerObservationConvention mongoOtelConvention() {
        return new MongoHandlerObservationConvention() {

            @Override public String getName() {
                // Span/Meter-Name der Observation
                return "db.mongodb.command";
            }

            @Override public String getContextualName(MongoHandlerContext ctx) {
                // „operation collection“ z.B. find notes
                return ctx.getCommand() + " " + (ctx.getCollection() == null ? "" : ctx.getCollection());
            }

            @Override public KeyValues getLowCardinalityKeyValues(MongoHandlerContext ctx) {
                return KeyValues.of(
                        KeyValue.of("db.system", "mongodb"),
                        KeyValue.of("db.operation", ctx.getCommand()),                     // OTel db.operation
                        KeyValue.of("db.name", nullSafe(ctx.getDatabase())),              // OTel db.name
                        KeyValue.of("db.mongodb.collection", nullSafe(ctx.getCollection())),
                        KeyValue.of("net.peer.name", nullSafe(ctx.getServerAddress())),   // host:port auflösen, wenn gewünscht
                        KeyValue.of("net.peer.port", ctx.getServerPort() == null ? "unknown" : String.valueOf(ctx.getServerPort()))
                );
            }

            @Override public boolean supportsContext(MongoHandlerContext ctx) { return true; }

            private String nullSafe(Object o) { return o == null ? "unknown" : String.valueOf(o); }
        };
    }

    // 2) Listener mit Convention registrieren
    @Bean
    MongoClientSettingsBuilderCustomizer mongoListenerCustomizer(
            ObservationRegistry registry,
            ConnectionString connectionString,
            MongoHandlerObservationConvention convention) {

        return settings -> settings.addCommandListener(
                new MongoObservationCommandListener(registry, connectionString, convention));
    }
}
