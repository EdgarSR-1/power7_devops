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
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TodoListRepository todoListRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final GroupMemberRepository groupMemberRepository;

    public TaskService(TaskRepository taskRepository,
                       TodoListRepository todoListRepository,
                       TaskGroupRepository taskGroupRepository,
                       UserRepository userRepository,
                       SprintRepository sprintRepository,
                       GroupMemberRepository groupMemberRepository) {
        this.taskRepository = taskRepository;
        this.todoListRepository = todoListRepository;
        this.taskGroupRepository = taskGroupRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.groupMemberRepository = groupMemberRepository;
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
            tasks = taskRepository.findAll().stream()
                    .filter(task -> {
                        Long taskGroupId = extractGroupId(task);
                        return canAccessGroup(currentUser, taskGroupId);
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
                    return canAccessGroup(currentUser, groupId);
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

    private boolean canAccessGroup(User user, Long groupId) {
        if (user == null || user.getId() == null || groupId == null) {
            return false;
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            return true;
        }

        if (taskGroupRepository.existsByIdAndCreatedById(groupId, user.getId())) {
            return true;
        }

        return taskRepository.existsByCreatedByIdAndTodoListGroupId(user.getId(), groupId);
    }

    private void validateGroupAccess(User currentUser, Long groupId) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (isSuperAdmin(currentUser)) {
            return;
        }

        if (!canAccessGroup(currentUser, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this group");
        }
    }

    private void validateTaskReadAccess(User currentUser, Task task) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (isSuperAdmin(currentUser)) {
            return;
        }

        Long groupId = extractGroupId(task);
        if (!canAccessGroup(currentUser, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this task");
        }
    }

    private void validateTaskEditAccess(User currentUser, Task task) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (isSuperAdmin(currentUser)) {
            return;
        }

        Long groupId = extractGroupId(task);

        if (isAdmin(currentUser)) {
            if (!canAccessGroup(currentUser, groupId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this task");
            }
            return;
        }

        if (isUsuario(currentUser)) {
            if (!canAccessGroup(currentUser, groupId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this task");
            }

            if (task.getCreatedBy() == null || !task.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own tasks");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    private TaskResponseDTO mapToResponseDTO(Task task) {
        String assigneeName = task.getCreatedBy() != null ? task.getCreatedBy().getName() : null;
        String todoListName = task.getTodoList() != null ? task.getTodoList().getName() : null;
        String groupName = null;
        Long sprintId = task.getSprint() != null ? task.getSprint().getId() : null;
        String sprintName = task.getSprint() != null ? task.getSprint().getName() : null;

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
                sprintName
        );
    }
}
