package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskAssignmentService {

    private final TaskAssignmentRepository repository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskAssignmentService(
            TaskAssignmentRepository repository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<TaskAssignment> getAll() {
        return repository.findAll();
    }

    public TaskAssignment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("TaskAssignment not found with id: " + id));
    }

    public List<TaskAssignment> getByTaskId(Long taskId) {
        return repository.findByTaskId(taskId);
    }

    public List<TaskAssignment> getByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public TaskAssignment save(TaskAssignment taskAssignment) {
        if (taskAssignment.getTask() == null || taskAssignment.getTask().getId() == null) {
            throw new RuntimeException("Task ID is required");
        }

        if (taskAssignment.getUser() == null || taskAssignment.getUser().getId() == null) {
            throw new RuntimeException("User ID is required");
        }

        Task task = taskRepository.findById(taskAssignment.getTask().getId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findById(taskAssignment.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        taskAssignment.setTask(task);
        taskAssignment.setUser(user);

        return repository.save(taskAssignment);
    }

    public TaskAssignment update(Long id, TaskAssignment updatedTaskAssignment) {
        TaskAssignment existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("TaskAssignment not found"));

        if (updatedTaskAssignment.getTask() != null && updatedTaskAssignment.getTask().getId() != null) {
            Task task = taskRepository.findById(updatedTaskAssignment.getTask().getId())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            existing.setTask(task);
        }

        if (updatedTaskAssignment.getUser() != null && updatedTaskAssignment.getUser().getId() != null) {
            User user = userRepository.findById(updatedTaskAssignment.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existing.setUser(user);
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}