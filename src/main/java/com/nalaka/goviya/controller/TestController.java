package com.nalaka.goviya.controller;

import com.nalaka.goviya.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final UserRepository repo;

    public TestController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/test-db")
    public String testDbConnection() {
        try {
            long count = repo.count();  // If DB is connected, this will work
            return "MongoDB connection SUCCESS. User count = " + count;
        } catch (Exception e) {
            return "MongoDB connection FAILED: " + e.getMessage();
        }
    }

}
