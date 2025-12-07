package com.nalaka.goviya.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

public class User {
    @Id
    private String id;

    private String name;
    private int age;
}
