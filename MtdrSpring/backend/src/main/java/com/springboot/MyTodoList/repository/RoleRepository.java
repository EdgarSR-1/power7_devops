package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}