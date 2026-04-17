package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameIgnoreCase(String name);

    Optional<Role> findFirstByOrderByIdAsc();
}
