package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskGroupRepository taskGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    public DebugController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/debug/db")
    public Map<String, Object> debugDb() {
        return jdbcTemplate.queryForMap(
            "select sys_context('USERENV','SESSION_USER') as USERNAME from dual"
        );
    }

    /**
     * Ver todos los grupos en el sistema
     * GET /api/debug/all-groups
     */
    @GetMapping("/api/debug/all-groups")
    public ResponseEntity<?> debugAllGroups() {
        try {
            List<TaskGroup> allGroups = taskGroupRepository.findAll();
            
            List<Map<String, Object>> response = allGroups.stream()
                .map(group -> {
                    Map<String, Object> groupInfo = new HashMap<>();
                    groupInfo.put("id", group.getId());
                    groupInfo.put("name", group.getName());
                    groupInfo.put("createdBy", group.getCreatedBy() != null ? group.getCreatedBy().getName() : "Unknown");
                    groupInfo.put("createdAt", group.getCreatedAt());
                    return groupInfo;
                })
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("totalGroups", allGroups.size());
            result.put("groups", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ver grupos accesibles para un usuario específico por telegramUserId
     * GET /api/debug/user-groups/{telegramUserId}
     * 
     * Retorna:
     * - Datos del usuario
     * - Grupos donde es miembro
     * - Grupos que creó
     * - Grupos donde creó tareas
     * - Total de grupos accesibles
     */
    @GetMapping("/api/debug/user-groups/{telegramUserId}")
    public ResponseEntity<?> debugUserGroups(@PathVariable Long telegramUserId) {
        try {
            // 1. Encontrar el usuario
            Optional<User> userOpt = userRepository.findByTelegramUserId(telegramUserId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado con telegramUserId: " + telegramUserId));
            }

            User user = userOpt.get();
            Long userId = user.getId();

            // 2. Grupos donde es miembro
            List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
            List<Map<String, Object>> membershipGroups = memberships.stream()
                .map(m -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", m.getGroup().getId());
                    info.put("name", m.getGroup().getName());
                    info.put("role", m.getRoleName());
                    info.put("joinedAt", m.getJoinedAt());
                    return info;
                })
                .collect(Collectors.toList());

            Set<Long> membershipGroupIds = memberships.stream()
                .map(m -> m.getGroup().getId())
                .collect(Collectors.toSet());

            // 3. Grupos que creó
            List<TaskGroup> createdGroups = taskGroupRepository.findByCreatedById(userId);
            List<Map<String, Object>> createdGroupsInfo = createdGroups.stream()
                .map(g -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", g.getId());
                    info.put("name", g.getName());
                    info.put("createdAt", g.getCreatedAt());
                    return info;
                })
                .collect(Collectors.toList());

            Set<Long> createdGroupIds = createdGroups.stream()
                .map(TaskGroup::getId)
                .collect(Collectors.toSet());

            // 4. Grupos donde tiene tareas
            List<Task> createdTasks = taskRepository.findByCreatedById(userId);
            Set<Long> groupsWithTasks = new HashSet<>();
            List<Map<String, Object>> tasksInfo = new ArrayList<>();

            for (Task task : createdTasks) {
                if (task.getTodoList() != null && task.getTodoList().getGroup() != null) {
                    Long groupId = task.getTodoList().getGroup().getId();
                    groupsWithTasks.add(groupId);

                    Map<String, Object> taskInfo = new HashMap<>();
                    taskInfo.put("taskId", task.getId());
                    taskInfo.put("taskName", task.getTitle());
                    taskInfo.put("groupId", groupId);
                    taskInfo.put("groupName", task.getTodoList().getGroup().getName());
                    taskInfo.put("status", task.getStatus());
                    tasksInfo.add(taskInfo);
                }
            }

            // 5. Calcular grupos totales accesibles
            Set<Long> allAccessibleGroupIds = new HashSet<>();
            allAccessibleGroupIds.addAll(membershipGroupIds);
            allAccessibleGroupIds.addAll(createdGroupIds);
            allAccessibleGroupIds.addAll(groupsWithTasks);

            // 6. Obtener detalles de todos los grupos accesibles
            List<TaskGroup> allAccessibleGroups = taskGroupRepository.findAllById(new ArrayList<>(allAccessibleGroupIds));
            List<Map<String, Object>> accessibleGroupsInfo = allAccessibleGroups.stream()
                .map(g -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", g.getId());
                    info.put("name", g.getName());
                    info.put("createdBy", g.getCreatedBy() != null ? g.getCreatedBy().getName() : "Unknown");
                    
                    // Tipo de acceso
                    List<String> accessTypes = new ArrayList<>();
                    if (membershipGroupIds.contains(g.getId())) {
                        accessTypes.add("MEMBER");
                    }
                    if (createdGroupIds.contains(g.getId())) {
                        accessTypes.add("CREATOR");
                    }
                    if (groupsWithTasks.contains(g.getId())) {
                        accessTypes.add("HAS_TASKS");
                    }
                    info.put("accessTypes", accessTypes);
                    return info;
                })
                .collect(Collectors.toList());

            // 7. Construir respuesta
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "telegramUserId", user.getTelegramUserId(),
                "telegramChatId", user.getTelegramChatId(),
                "role", user.getRole() != null ? user.getRole().getName() : "Unknown"
            ));

            response.put("summary", Map.of(
                "totalGroupsInSystem", taskGroupRepository.findAll().size(),
                "totalAccessibleGroups", allAccessibleGroupIds.size(),
                "membershipGroups", membershipGroupIds.size(),
                "createdGroups", createdGroupIds.size(),
                "groupsWithTasks", groupsWithTasks.size(),
                "totalTasks", createdTasks.size()
            ));

            response.put("accessibleGroups", accessibleGroupsInfo);
            response.put("membershipDetails", membershipGroups);
            response.put("createdGroupsDetails", createdGroupsInfo);
            response.put("tasksDetails", tasksInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
        }
    }
}