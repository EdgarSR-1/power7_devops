package com.springboot.MyTodoList.repository;

import java.util.Optional;
import com.springboot.MyTodoList.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByTelegramUserId(Long telegramUserId);
}