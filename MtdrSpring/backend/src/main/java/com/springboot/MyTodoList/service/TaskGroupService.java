package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.RoleName;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.RoleRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGroupService {

    private final TaskGroupRepository repository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RoleRepository roleRepository;
    private final TodoListRepository todoListRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskGroupService(
            TaskGroupRepository repository,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository,
            RoleRepository roleRepository,
            TodoListRepository todoListRepository,
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.roleRepository = roleRepository;
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

    public TaskGroup createGroupForBot(String groupName, User requesterUser) {
        TaskGroup group = new TaskGroup();
        group.setName(groupName);
        group.setCreatedBy(resolveGroupOwner());
        TaskGroup savedGroup = repository.save(group);

        if (requesterUser != null && requesterUser.getId() != null
                && !groupMemberRepository.existsByGroupIdAndUserId(savedGroup.getId(), requesterUser.getId())) {
            GroupMember member = new GroupMember();
            member.setGroup(savedGroup);
            member.setUser(requesterUser);
            Role role = resolveMemberRole(requesterUser);
            member.setRole(role);
            member.setRoleName(role.getName().name());
            groupMemberRepository.save(member);
        }

        return savedGroup;
    }

    public List<TaskGroup> findAccessibleGroups(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return Collections.emptyList();
        }

        if (isSuperAdmin(currentUser)) {
            return repository.findAll();
        }

        Map<Long, TaskGroup> uniqueGroups = new LinkedHashMap<>();
        List<GroupMember> memberships = groupMemberRepository.findByUserId(currentUser.getId());
        for (GroupMember membership : memberships) {
            if (membership.getGroup() != null && membership.getGroup().getId() != null) {
                uniqueGroups.putIfAbsent(membership.getGroup().getId(), membership.getGroup());
            }
        }

        return new ArrayList<>(uniqueGroups.values());
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

    private boolean isSuperAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.SUPERADMIN;
    }

    private Role resolveMemberRole(User requesterUser) {
        RoleName roleName = RoleName.USUARIO;
        if (requesterUser != null && requesterUser.getRole() != null && requesterUser.getRole().getName() != null) {
            RoleName userRole = requesterUser.getRole().getName();
            if (userRole == RoleName.ADMIN || userRole == RoleName.SUPERADMIN) {
                roleName = userRole;
            }
        }

        RoleName finalRoleName = roleName;
        return roleRepository.findByName(finalRoleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(finalRoleName);
            role.setDescription("Group member role");
            return roleRepository.save(role);
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
