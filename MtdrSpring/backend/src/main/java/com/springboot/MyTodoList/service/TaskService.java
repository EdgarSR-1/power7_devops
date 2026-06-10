package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.TaskRequestDTO;
import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.RoleName;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskPriority;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class TaskService {

    private static final int AI_TASK_LIMIT = 30;

    private final TaskRepository taskRepository;
    private final TodoListRepository todoListRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskService(TaskRepository taskRepository,
                       TodoListRepository todoListRepository,
                       TaskGroupRepository taskGroupRepository,
                       UserRepository userRepository,
                       SprintRepository sprintRepository,
                       GroupMemberRepository groupMemberRepository,
                       TaskAssignmentRepository taskAssignmentRepository) {
        this.taskRepository = taskRepository;
        this.todoListRepository = todoListRepository;
        this.taskGroupRepository = taskGroupRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public TaskResponseDTO createTask(TaskRequestDTO dto, User currentUser) {
        TodoList todoList = todoListRepository.findById(dto.getListId())
                .orElseThrow(() -> new RuntimeException("TodoList not found"));

        Long groupId = extractGroupId(todoList);
        validateGroupAccess(currentUser, groupId);

        User createdBy = currentUser;
        if (dto.getCreatedById() != null) {
            User requestedCreator = userRepository.findById(dto.getCreatedById())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (isSuperAdmin(currentUser)) {
                createdBy = requestedCreator;
            } else if (requestedCreator.getId().equals(currentUser.getId())) {
                createdBy = currentUser;
            } else {
                throw new RuntimeException("You cannot create tasks for another user");
            }
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        Sprint sprint = resolveSprint(dto);

        Task task = new Task();
        task.setTodoList(todoList);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());

        if (dto.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(dto.getStatus()));
        } else {
            task.setStatus(TaskStatus.pending);
        }

        if (dto.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(dto.getPriority()));
        } else {
            task.setPriority(TaskPriority.medium);
        }

        task.setStartDate(dto.getStartDate());
        task.setEndDate(dto.getEndDate());
        task.setDueDate(dto.getDueDate());
        task.setCreatedBy(createdBy);
        task.setSprint(sprint);
        task.setEstimatedHours(dto.getEstimatedHours());

        Task savedTask = taskRepository.save(task);
        return mapToResponseDTO(savedTask);
    }

    private Sprint resolveSprint(TaskRequestDTO dto) {
        if (dto.getSprintId() != null) {
            return sprintRepository.findById(dto.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint not found"));
        }

        return resolveSprintForWindow(dto.getStartDate(), dto.getEndDate(), dto.getDueDate());
    }

    private User getUserByEmailForAi(String userEmail) {
    if (userEmail == null || userEmail.trim().isEmpty()) {
        throw new RuntimeException("Unauthorized");
    }

    return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
}

private TaskStatus parseTaskStatusForAi(String status) {
    if (status == null || status.trim().isEmpty()) {
        throw new RuntimeException("Task status is required");
    }

    String normalizedStatus = status.trim().toLowerCase();

    if ("pending".equals(normalizedStatus)
            || "pendiente".equals(normalizedStatus)
            || "pendientes".equals(normalizedStatus)) {
        return TaskStatus.pending;
    }

    if ("in_progress".equals(normalizedStatus)
            || "in progress".equals(normalizedStatus)
            || "progreso".equals(normalizedStatus)
            || "en progreso".equals(normalizedStatus)) {
        return TaskStatus.in_progress;
    }

    if ("completed".equals(normalizedStatus)
            || "complete".equals(normalizedStatus)
            || "completada".equals(normalizedStatus)
            || "completadas".equals(normalizedStatus)
            || "terminada".equals(normalizedStatus)
            || "terminadas".equals(normalizedStatus)) {
        return TaskStatus.completed;
    }

    throw new RuntimeException("Invalid task status: " + status);
    }

    private Sprint resolveSprintForWindow(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime dueDate) {
        LocalDateTime rangeStart = startDate;
        LocalDateTime rangeEnd = endDate;

        if (rangeStart == null && dueDate != null) {
            rangeStart = dueDate;
        }

        if (rangeEnd == null && dueDate != null) {
            rangeEnd = dueDate;
        }

        if (rangeStart == null && rangeEnd != null) {
            rangeStart = rangeEnd;
        }

        if (rangeEnd == null && rangeStart != null) {
            rangeEnd = rangeStart;
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
            rangeEnd = rangeStart;
        }

        if (rangeEnd.isBefore(rangeStart)) {
            throw new RuntimeException("End date cannot be before start date");
        }

        return sprintRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(rangeStart, rangeEnd)
                .orElse(null);
    }

    public List<TaskResponseDTO> getAllTasks(User currentUser) {
        List<Task> tasks;

        if (isSuperAdmin(currentUser)) {
            tasks = taskRepository.findAll();
        } else {
            List<GroupMember> memberships = groupMemberRepository.findByUserId(currentUser.getId());
            List<Long> groupIds = memberships.stream()
                    .map(groupMember -> groupMember.getGroup().getId())
                    .collect(Collectors.toList());

            tasks = taskRepository.findAll().stream()
                    .filter(task -> {
                        Long taskGroupId = extractGroupId(task);
                        return taskGroupId != null && groupIds.contains(taskGroupId);
                    })
                    .collect(Collectors.toList());
        }

        return tasks.stream()
                .sorted(Comparator
                        .comparing((Task task) -> task.getTodoList() != null && task.getTodoList().getGroup() != null
                                ? task.getTodoList().getGroup().getName()
                                : "")
                        .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO getTaskById(Long id, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskReadAccess(currentUser, task);
        return mapToResponseDTO(task);
    }

    public Optional<Sprint> getCurrentSprint() {
        LocalDateTime now = LocalDateTime.now();
        return sprintRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(now, now);
    }

    public Sprint getSprintById(Long sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
    }

    public List<Task> getTasksBySprintId(Long sprintId, User currentUser) {
        List<Task> tasks = taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId);

        if (isSuperAdmin(currentUser)) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> {
                    Long groupId = extractGroupId(task);
                    return belongsToGroup(currentUser, groupId);
                })
                .collect(Collectors.toList());
    }

    public TaskResponseDTO updateTaskStatus(Long taskId, TaskStatus status, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskEditAccess(currentUser, task);
        task.setStatus(status);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO moveTaskToSprint(Long taskId, Long sprintId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskEditAccess(currentUser, task);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        task.setSprint(sprint);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public void deleteTask(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskEditAccess(currentUser, task);
        taskRepository.delete(task);
    }

    public List<Task> getTasksByGroupId(Long groupId, User currentUser) {
        validateGroupAccess(currentUser, groupId);
        return taskRepository.findByTodoListGroupId(groupId);
    }

    public TaskResponseDTO createTaskInGroup(Long groupId, String title, User currentUser) {
        validateGroupAccess(currentUser, groupId);

        TaskGroup group = taskGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("TaskGroup not found"));

        TodoList targetList = todoListRepository.findByGroupId(groupId).stream().findFirst().orElseGet(() -> {
            TodoList createdList = new TodoList();
            createdList.setGroup(group);
            createdList.setName("General");
            createdList.setCreatedBy(group.getCreatedBy());
            return todoListRepository.save(createdList);
        });

        Sprint sprint = resolveSprintForWindow(null, null, null);

        Task task = new Task();
        task.setTodoList(targetList);
        task.setTitle(title);
        task.setDescription(title);
        task.setStatus(TaskStatus.pending);
        task.setPriority(TaskPriority.medium);
        task.setCreatedBy(currentUser);
        task.setSprint(sprint);

        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO createTaskInGroupWithHours(Long groupId, String title, Float estimatedHours, User currentUser) {
        validateGroupAccess(currentUser, groupId);

        TaskGroup group = taskGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("TaskGroup not found"));

        TodoList targetList = todoListRepository.findByGroupId(groupId).stream().findFirst().orElseGet(() -> {
            TodoList createdList = new TodoList();
            createdList.setGroup(group);
            createdList.setName("General");
            createdList.setCreatedBy(group.getCreatedBy());
            return todoListRepository.save(createdList);
        });

        Sprint sprint = resolveSprintForWindow(null, null, null);

        Task task = new Task();
        task.setTodoList(targetList);
        task.setTitle(title);
        task.setDescription(title);
        task.setStatus(TaskStatus.pending);
        task.setPriority(TaskPriority.medium);
        task.setCreatedBy(currentUser);
        task.setSprint(sprint);
        task.setEstimatedHours(estimatedHours != null ? estimatedHours : 1f);

        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO startTask(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskEditAccess(currentUser, task);
        task.setStatus(TaskStatus.in_progress);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO completeTask(Long taskId, Float actualHours, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskEditAccess(currentUser, task);
        task.setStatus(TaskStatus.completed);
        task.setActualHours(actualHours);
        return mapToResponseDTO(taskRepository.save(task));
    }

    private boolean isSuperAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.SUPERADMIN;
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.ADMIN;
    }

    private boolean isUsuario(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.USUARIO;
    }

    private Long extractGroupId(Task task) {
        if (task == null || task.getTodoList() == null || task.getTodoList().getGroup() == null) {
            return null;
        }
        return task.getTodoList().getGroup().getId();
    }

    private Long extractGroupId(TodoList todoList) {
        if (todoList == null || todoList.getGroup() == null) {
            return null;
        }
        return todoList.getGroup().getId();
    }

    private boolean belongsToGroup(User user, Long groupId) {
        if (user == null || user.getId() == null || groupId == null) {
            return false;
        }

        return groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId());
    }

    private void validateGroupAccess(User currentUser, Long groupId) {
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized");
        }

        if (isSuperAdmin(currentUser)) {
            return;
        }

        if (!belongsToGroup(currentUser, groupId)) {
            throw new RuntimeException("You do not have access to this group");
        }
    }

    private void validateTaskReadAccess(User currentUser, Task task) {
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized");
        }

        if (isSuperAdmin(currentUser)) {
            return;
        }

        Long groupId = extractGroupId(task);
        if (!belongsToGroup(currentUser, groupId)) {
            throw new RuntimeException("You do not have access to this task");
        }
    }

  private void validateTaskEditAccess(User currentUser, Task task) {

    if (currentUser == null) {
        throw new RuntimeException("Unauthorized");
    }

    if (isSuperAdmin(currentUser)) {
        return;
    }

    Long groupId = extractGroupId(task);

    if (isAdmin(currentUser)) {
        if (!belongsToGroup(currentUser, groupId)) {
            throw new RuntimeException("You do not have access to this task");
        }
        return;
    }

    if (isUsuario(currentUser)) {
        if (!belongsToGroup(currentUser, groupId)) {
            throw new RuntimeException("You do not have access to this task");
        }

        boolean isAssignedToTask = taskAssignmentRepository.existsByTaskIdAndUserId(
                task.getId(),
                currentUser.getId()
        );

        if (!isAssignedToTask) {
            throw new RuntimeException("You can only modify tasks assigned to you");
        }

        return;
    }

    

    throw new RuntimeException("Unauthorized");
    }

    public List<Task> getOverdueTasksForUser(String userEmail) {
    User user = getUserByEmailForAi(userEmail);
    Pageable limit = PageRequest.of(0, AI_TASK_LIMIT);
    LocalDateTime now = LocalDateTime.now();

    if (isSuperAdmin(user)) {
        return taskRepository.findOverdueTasksForSuperAdmin(
                now,
                TaskStatus.completed,
                limit
        );
    }

    return taskRepository.findOverdueTasksVisibleToUser(
            user.getId(),
            now,
            TaskStatus.completed,
            limit
    );
}

public List<Task> getTasksForUserByStatus(String userEmail, String status) {
    User user = getUserByEmailForAi(userEmail);
    TaskStatus parsedStatus = parseTaskStatusForAi(status);
    Pageable limit = PageRequest.of(0, AI_TASK_LIMIT);

    if (isSuperAdmin(user)) {
        return taskRepository.findTasksByStatusForSuperAdmin(
                parsedStatus,
                limit
        );
    }

    return taskRepository.findTasksByStatusVisibleToUser(
            user.getId(),
            parsedStatus,
            limit
    );
}

public List<Task> getHighPriorityTasksForUser(String userEmail) {
    User user = getUserByEmailForAi(userEmail);
    Pageable limit = PageRequest.of(0, AI_TASK_LIMIT);

    if (isSuperAdmin(user)) {
        return taskRepository.findTasksByPriorityForSuperAdmin(
                TaskPriority.high,
                TaskStatus.completed,
                limit
        );
    }

    return taskRepository.findTasksByPriorityVisibleToUser(
            user.getId(),
            TaskPriority.high,
            TaskStatus.completed,
            limit
    );
}

public List<Task> getTasksBySprintForUser(String userEmail, Long sprintId) {
    if (sprintId == null) {
        throw new RuntimeException("Sprint id is required");
    }

    User user = getUserByEmailForAi(userEmail);
    Pageable limit = PageRequest.of(0, AI_TASK_LIMIT);

    if (isSuperAdmin(user)) {
        return taskRepository.findTasksBySprintForSuperAdmin(
                sprintId,
                limit
        );
    }

    return taskRepository.findTasksBySprintVisibleToUser(
            user.getId(),
            sprintId,
            limit
    );
}

    private TaskResponseDTO mapToResponseDTO(Task task) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());
        String assigneeName = assignments.stream()
            .map(a -> a.getUser().getName())
            .collect(Collectors.joining(", "));
        String todoListName = task.getTodoList() != null ? task.getTodoList().getName() : null;
        String groupName = null;
        Long sprintId = task.getSprint() != null ? task.getSprint().getId() : null;
        String sprintName = task.getSprint() != null ? task.getSprint().getName() : null;
        Float estimatedHours = task.getEstimatedHours();

        if (task.getTodoList() != null && task.getTodoList().getGroup() != null) {
            groupName = task.getTodoList().getGroup().getName();
        }

        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus() != null ? task.getStatus().name() : null,
                task.getPriority() != null ? task.getPriority().name() : null,
                task.getStartDate(),
                task.getEndDate(),
                task.getDueDate(),
                task.getCreatedAt(),
                groupName,
                todoListName,
                assigneeName,
                sprintId,
                sprintName,
                estimatedHours
        );
    }
}