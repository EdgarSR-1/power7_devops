package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoListRepository extends JpaRepository<TodoList, Long> {
    List<TodoList> findByGroupId(Long groupId);
    List<TodoList> findByCreatedById(Long userId);
}