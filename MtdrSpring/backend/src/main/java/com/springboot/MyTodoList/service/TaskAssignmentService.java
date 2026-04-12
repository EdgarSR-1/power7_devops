package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskAssignmentService {

    private final TaskAssignmentRepository repository;

    public TaskAssignmentService(TaskAssignmentRepository repository) {
        this.repository = repository;
    }

    public TaskAssignment save(TaskAssignment assignment) {
        return repository.save(assignment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}