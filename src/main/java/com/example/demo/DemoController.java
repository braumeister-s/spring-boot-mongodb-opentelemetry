package com.example.demo;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/notes")
public class DemoController {

    private final DemoRepository repo;

    @PostMapping
    public ResponseEntity<Note> create(@RequestBody Note note) {
        note = repo.save(note);
        return ResponseEntity.created(URI.create("/api/notes/" + note.getId())).body(note);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> get(@PathVariable String id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Note> all() { return repo.findAll(); }

    @PutMapping("/{id}")
    public ResponseEntity<Note> update(@PathVariable String id, @RequestBody Note note) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        note.setId(id);
        return ResponseEntity.ok(repo.save(note));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        repo.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
