package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DemoRepository extends MongoRepository<Note, String> {
}