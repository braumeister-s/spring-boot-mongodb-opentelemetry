package com.example.mongo.otel;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document(collection = "notes")
public class Note {
    @Id
    private String id;
    @NonNull
    private String title;
    private String content;
}
