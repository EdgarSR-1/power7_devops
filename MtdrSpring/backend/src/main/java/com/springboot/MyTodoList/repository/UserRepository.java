package com.springboot.MyTodoList.repository;

import java.util.Optional;
import com.springboot.MyTodoList.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}