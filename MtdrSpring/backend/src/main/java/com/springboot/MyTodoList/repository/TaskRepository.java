package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTodoListId(Long listId);
    List<Task> findByCreatedById(Long userId);
}