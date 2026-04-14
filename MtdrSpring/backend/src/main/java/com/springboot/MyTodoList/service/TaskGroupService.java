package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskGroupService {

    private final TaskGroupRepository repository;
    private final UserRepository userRepository;

    public TaskGroupService(TaskGroupRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<TaskGroup> findAll() {
        return repository.findAll();
    }

    public TaskGroup save(TaskGroup group) {
        return repository.save(group);
    }

    public TaskGroup createGroupForBot(String groupName) {
        TaskGroup group = new TaskGroup();
        group.setName(groupName);
        group.setCreatedBy(resolveGroupOwner());
        return repository.save(group);
    }

    private User resolveGroupOwner() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User botOwner = new User();
            botOwner.setName("Bot Owner");
            botOwner.setEmail("bot-owner@local.test");
            botOwner.setPassword("bot-owner-temp");
            return userRepository.save(botOwner);
        });
    }

    public TaskGroup findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}