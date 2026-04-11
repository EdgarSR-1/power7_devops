package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskAssignmentRepository repository;

    public TaskAssignment save(TaskAssignment assignment) {
        return repository.save(assignment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}