package com.nalaka.goviya.repository;

import com.nalaka.goviya.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User,String> {
    boolean existsByEmail(String email);
}
