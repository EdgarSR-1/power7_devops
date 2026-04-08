package com.springboot.MyTodoList.services;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskGroupService {

    private final TaskGroupRepository repository;

    public List<TaskGroup> findAll() {
        return repository.findAll();
    }

    public TaskGroup save(TaskGroup group) {
        return repository.save(group);
    }

    public TaskGroup findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}