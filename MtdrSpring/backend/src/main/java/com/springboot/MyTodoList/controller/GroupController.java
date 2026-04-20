package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.UserGroupResponseDTO;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:3000")
public class GroupController {

    private final GroupMemberRepository groupMemberRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final TaskRepository taskRepository;
    private final JwtService jwtService;

    public GroupController(GroupMemberRepository groupMemberRepository,
                           TaskGroupRepository taskGroupRepository,
                           TaskRepository taskRepository,
                           JwtService jwtService) {
        this.groupMemberRepository = groupMemberRepository;
        this.taskGroupRepository = taskGroupRepository;
        this.taskRepository = taskRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUserGroups(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }

        final Long userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Could not parse user from token");
        }

        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);

        Map<Long, TaskGroup> uniqueGroups = new LinkedHashMap<>();
        for (GroupMember membership : memberships) {
            if (membership.getGroup() != null && membership.getGroup().getId() != null) {
                uniqueGroups.putIfAbsent(membership.getGroup().getId(), membership.getGroup());
            }
        }

        List<TaskGroup> createdGroups = taskGroupRepository.findByCreatedById(userId);
        for (TaskGroup group : createdGroups) {
            if (group != null && group.getId() != null) {
                uniqueGroups.putIfAbsent(group.getId(), group);
            }
        }

        List<Task> createdTasks = taskRepository.findByCreatedById(userId);
        for (Task task : createdTasks) {
            if (task.getTodoList() != null && task.getTodoList().getGroup() != null) {
                TaskGroup group = task.getTodoList().getGroup();
                if (group.getId() != null) {
                    uniqueGroups.putIfAbsent(group.getId(), group);
                }
            }
        }

        List<UserGroupResponseDTO> response = new ArrayList<>();
        for (TaskGroup group : uniqueGroups.values()) {
            List<Task> tasks = taskRepository.findByTodoListGroupId(group.getId());
            int totalTasks = tasks.size();
            long completedTasks = tasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.completed)
                    .count();

            int progress = totalTasks == 0
                    ? 0
                    : (int) Math.round((completedTasks * 100.0) / totalTasks);

            List<String> members = groupMemberRepository.findByGroupId(group.getId())
                    .stream()
                    .map(member -> member.getUser() != null ? member.getUser().getName() : null)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            response.add(new UserGroupResponseDTO(
                    group.getId(),
                    group.getName(),
                    "",
                    progress,
                    totalTasks,
                    members
            ));
        }

        return ResponseEntity.ok(response);
    }
}