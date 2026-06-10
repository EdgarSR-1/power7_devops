package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.RoleName;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.RoleRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
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
        String password = encodePasswordIfNeeded(normalizeRequired(user.getPassword(), "Password is required"));
        String phone = normalizePhoneRequired(user.getPhone(), "Phone is required");
        Long telegramUserId = user.getTelegramUserId();
        Long telegramChatId = user.getTelegramChatId();
        Role defaultRole = resolveDefaultRole();

        if (telegramUserId != null) {
            Optional<User> existingByTelegram = repo.findByTelegramUserId(telegramUserId);
            if (existingByTelegram.isPresent()) {
                User existing = existingByTelegram.get();
                ensureEmailAvailable(email, existing.getId());

                existing.setName(name);
                existing.setEmail(email);
                existing.setPassword(password);
                existing.setPhone(phone);
                existing.setTelegramChatId(telegramChatId);
                if (existing.getRole() == null) {
                    existing.setRole(defaultRole);
                }
                return repo.save(existing);
            }

            Optional<User> existingByPhone = repo.findByPhone(phone);
            if (existingByPhone.isPresent()) {
                User existing = existingByPhone.get();
                if (existing.getTelegramUserId() != null && !existing.getTelegramUserId().equals(telegramUserId)) {
                    throw new RuntimeException("Phone already linked to another Telegram account");
                }

                ensureEmailAvailable(email, existing.getId());

                existing.setName(name);
                existing.setEmail(email);
                existing.setPassword(password);
                existing.setPhone(phone);
                existing.setTelegramUserId(telegramUserId);
                existing.setTelegramChatId(telegramChatId);
                if (existing.getRole() == null) {
                    existing.setRole(defaultRole);
                }
                return repo.save(existing);
            }
        }

        // Caso: el telegramUserId no está en BD y el teléfono tampoco,
        // pero el email sí existe → vincular Telegram a ese usuario existente
        Optional<User> existingByEmail = repo.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();

            // Si ese usuario ya tiene otro Telegram vinculado, rechazar
            if (existing.getTelegramUserId() != null
                    && !existing.getTelegramUserId().equals(telegramUserId)) {
                throw new RuntimeException(
                    "Este usuario ya está vinculado a otra cuenta de Telegram");
            }

            // Vincular tu Telegram a este usuario existente
            existing.setTelegramUserId(telegramUserId);
            existing.setTelegramChatId(telegramChatId);
            // Actualizar teléfono si el usuario no tenía
            if (existing.getPhone() == null || existing.getPhone().isBlank()) {
                existing.setPhone(phone);
            }
            if (existing.getRole() == null) {
                existing.setRole(defaultRole);
            }
            return repo.save(existing);
        }

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);

        if (user.getRole() == null) {
            user.setRole(defaultRole);
        }

        return repo.save(user);
    }

    private Role resolveDefaultRole() {
        return roleRepository.findByName(RoleName.USUARIO).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleName.USUARIO);
            role.setDescription("Default user role");
            return roleRepository.save(role);
        });
    }

    public Optional<User> findByTelegramUserId(Long telegramUserId) {
        if (telegramUserId == null) {
            return Optional.empty();
        }
        return repo.findByTelegramUserId(telegramUserId);
    }

    public User loginTelegramByEmailPassword(String rawEmail, String rawPassword, Long telegramUserId, Long telegramChatId) {
        if (telegramUserId == null) {
            throw new RuntimeException("Telegram identity is required");
        }

        String email = normalizeRequired(rawEmail, "Email is required").toLowerCase(Locale.ROOT);
        String password = normalizeRequired(rawPassword, "Password is required");

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getTelegramUserId() != null && !user.getTelegramUserId().equals(telegramUserId)) {
            throw new RuntimeException("Telegram account already linked");
        }

        Optional<User> existingTelegramUser = repo.findByTelegramUserId(telegramUserId);
        if (existingTelegramUser.isPresent() && !existingTelegramUser.get().getId().equals(user.getId())) {
            User previousUser = existingTelegramUser.get();
            previousUser.setTelegramUserId(null);
            previousUser.setTelegramChatId(null);
            repo.save(previousUser);
        }

        user.setTelegramUserId(telegramUserId);
        user.setTelegramChatId(telegramChatId);
        return repo.save(user);
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

    private void ensureEmailAvailable(String email, Long excludedUserId) {
        Optional<User> existingByEmail = repo.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (excludedUserId == null || !existing.getId().equals(excludedUserId)) {
                throw new RuntimeException("Email already exists");
            }
        }
    }

    private String encodePasswordIfNeeded(String password) {
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }

        return passwordEncoder.encode(password);
    }

    public boolean unlinkTelegram(Long telegramUserId) {
        if (telegramUserId == null) return false;
        
        Optional<User> existing = repo.findByTelegramUserId(telegramUserId);
        if (existing.isEmpty()) return false;
        
        User user = existing.get();
        user.setTelegramUserId(null);
        user.setTelegramChatId(null);
        repo.save(user);
        return true;
    }
}
