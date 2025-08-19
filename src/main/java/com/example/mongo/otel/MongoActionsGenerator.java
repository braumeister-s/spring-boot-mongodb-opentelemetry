package com.example.mongo.otel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@AllArgsConstructor
@Slf4j
@EnableScheduling
public class MongoActionsGenerator {

    private DemoRepository repo;

    @Scheduled(fixedRate = 10000)
    public void runScenario() {

        log.info("MongoDB - Testdaten Generierung gestartet");
        // 1) Lesen
        List<Note> before = repo.findAll();

        // 2) 5 anlegen
        List<Note> batch = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new Note("Demo Note " + i))
                .toList();

        List<Note> saved = repo.saveAll(batch);

        // 3) alles löschen
        repo.deleteAll();
    }

    @Data
    @AllArgsConstructor
    private class Result {
        private final List<Note> existingBefore;
        private final List<Note> insertedNow;
    }

}