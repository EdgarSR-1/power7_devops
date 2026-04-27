package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGroupService {

    private final TaskGroupRepository repository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TodoListRepository todoListRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskGroupService(
            TaskGroupRepository repository,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository,
            TodoListRepository todoListRepository,
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.todoListRepository = todoListRepository;
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public List<TaskGroup> findAll() {
        return repository.findAll();
    }

    public TaskGroup save(TaskGroup group) {
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }

        group.setName(group.getName().trim());
        if (group.getCreatedBy() == null) {
            group.setCreatedBy(resolveGroupOwner());
        }

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

    @Transactional
    public void delete(Long id) {
        findById(id);

        List<Task> tasks = taskRepository.findByTodoListGroupId(id);
        for (Task task : tasks) {
            taskAssignmentRepository.deleteAll(taskAssignmentRepository.findByTaskId(task.getId()));
        }

        taskRepository.deleteAll(tasks);

        List<TodoList> todoLists = todoListRepository.findByGroupId(id);
        todoListRepository.deleteAll(todoLists);
        groupMemberRepository.deleteAll(groupMemberRepository.findByGroupId(id));
        repository.deleteById(id);
    }
}
