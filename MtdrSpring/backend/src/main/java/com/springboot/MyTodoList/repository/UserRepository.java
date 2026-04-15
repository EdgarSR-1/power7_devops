package com.springboot.MyTodoList.repository;

import java.util.Optional;
import com.springboot.MyTodoList.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
<<<<<<< HEAD
=======
    Optional<User> findByEmail(String email);
>>>>>>> d876586476327f2378ddd5d7f84848b8b7b7e8e0
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByTelegramUserId(Long telegramUserId);
}