package com.example.mongo.otel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@AllArgsConstructor
@Slf4j
public class StartupRunner implements CommandLineRunner {

    private DemoRepository repo;

    @Override
    public void run(String... args) {

        var result = runScenario();
        log.info("Vorhandene Notes vor dem Einfügen: {}", result.getExistingBefore().size());
        log.info("Gerade eingefügte Notes: {}", result.getInsertedNow().size());
        log.info("Danach wurde alles wieder gelöscht.");
    }

    public Result runScenario() {

        // 1) Lesen
        List<Note> before = repo.findAll();

        // 2) 5 anlegen
        List<Note> batch = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new Note("Demo Note " + i))
                .toList();

        List<Note> saved = repo.saveAll(batch);

        // 3) alles löschen
        repo.deleteAll();

        return new Result(before, saved);
    }

    @Data
    @AllArgsConstructor
    private class Result {
        private final List<Note> existingBefore;
        private final List<Note> insertedNow;
    }

}