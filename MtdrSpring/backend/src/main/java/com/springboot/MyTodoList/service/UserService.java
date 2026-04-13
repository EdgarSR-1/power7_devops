package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        String phone = normalizePhoneRequired(user.getPhone(), "Phone is required");

        if (repo.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        return repo.save(user);
    }

    public Optional<User> findByTelegramUserId(Long telegramUserId) {
        if (telegramUserId == null) {
            return Optional.empty();
        }
        return repo.findByTelegramUserId(telegramUserId);
    }

    public boolean linkTelegramIdentityByPhone(Long telegramUserId, Long telegramChatId, String rawPhone) {
        if (telegramUserId == null || rawPhone == null || rawPhone.trim().isEmpty()) {
            return false;
        }

        String normalizedPhone = normalizePhoneRequired(rawPhone, "Phone is required");

        Optional<User> byTelegram = repo.findByTelegramUserId(telegramUserId);
        if (byTelegram.isPresent()) {
            User existing = byTelegram.get();
            existing.setTelegramChatId(telegramChatId);
            if (existing.getPhone() == null || existing.getPhone().trim().isEmpty()) {
                existing.setPhone(normalizedPhone);
            }
            repo.save(existing);
            return true;
        }

        Optional<User> byPhone = repo.findByPhone(normalizedPhone);
        if (byPhone.isEmpty()) {
            return false;
        }

        User user = byPhone.get();
        if (user.getTelegramUserId() != null && !user.getTelegramUserId().equals(telegramUserId)) {
            return false;
        }

        user.setTelegramUserId(telegramUserId);
        user.setTelegramChatId(telegramChatId);
        user.setPhone(normalizedPhone);
        repo.save(user);
        return true;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String normalizePhoneRequired(String value, String message) {
        String phone = normalizeRequired(value, message);
        String compact = phone.replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (compact.startsWith("+")) {
            String digits = compact.substring(1).replaceAll("\\D", "");
            if (digits.isEmpty()) {
                throw new IllegalArgumentException(message);
            }
            return "+" + digits;
        }

        String digits = compact.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return digits;
    }
}