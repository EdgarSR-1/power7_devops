package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<User> getAllUsers() {
        return repo.findAll();
    }

    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User data is required");
        }

        String name = normalizeRequired(user.getName(), "Name is required");
        String email = normalizeRequired(user.getEmail(), "Email is required").toLowerCase(Locale.ROOT);
        String password = normalizeRequired(user.getPassword(), "Password is required");

        if (repo.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        return repo.save(user);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}