package com.springboot.MyTodoList.services;

import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
