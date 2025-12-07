package com.nalaka.goviya.service;


import com.nalaka.goviya.model.User;
import com.nalaka.goviya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    public User register(User user) {
        if(repo.existsByEmail(user.getEmail())){
            throw new RuntimeException("User already exists");
        }

        return repo.save(user);
    }

}
