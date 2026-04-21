package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.TaskRequestDTO;
import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskPriority;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TodoListRepository todoListRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;

    public TaskService(TaskRepository taskRepository,
                       TodoListRepository todoListRepository,
                       TaskGroupRepository taskGroupRepository,
                       UserRepository userRepository,
                       SprintRepository sprintRepository) {
        this.taskRepository = taskRepository;
        this.todoListRepository = todoListRepository;
        this.taskGroupRepository = taskGroupRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
    }

    public TaskResponseDTO createTask(TaskRequestDTO dto) {
        TodoList todoList = todoListRepository.findById(dto.getListId())
                .orElseThrow(() -> new RuntimeException("TodoList not found"));

        User createdBy = null;
        if (dto.getCreatedById() != null) {
            createdBy = userRepository.findById(dto.getCreatedById())
                    .orElseThrow(() -> new RuntimeException("User not found"));
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
        }

        if (dto.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(dto.getPriority()));
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

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .sorted(Comparator
                        .comparing((Task task) -> task.getTodoList() != null && task.getTodoList().getGroup() != null
                                ? task.getTodoList().getGroup().getName()
                                : "")
                        .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
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

    public List<Task> getTasksBySprintId(Long sprintId) {
        return taskRepository.findBySprintIdOrderByCreatedAtAsc(sprintId);
    }

    public TaskResponseDTO updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO moveTaskToSprint(Long taskId, Long sprintId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        task.setSprint(sprint);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    public List<Task> getTasksByGroupId(Long groupId) {
        return taskRepository.findByTodoListGroupId(groupId);
    }

    public TaskResponseDTO createTaskInGroup(Long groupId, String title) {
        return createTaskInGroup(groupId, title, null);
    }

    public TaskResponseDTO createTaskInGroup(Long groupId, String title, User createdBy) {
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
        User taskCreator = createdBy != null ? createdBy : group.getCreatedBy();

        Task task = new Task();
        task.setTodoList(targetList);
        task.setTitle(title);
        task.setDescription(title);
        task.setStatus(TaskStatus.pending);
        task.setPriority(TaskPriority.medium);
        task.setCreatedBy(taskCreator);
        task.setSprint(sprint);

        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO createTaskInGroupWithHours(Long groupId, String title, Float estimatedHours) {
        return createTaskInGroupWithHours(groupId, title, estimatedHours, null);
    }

    public TaskResponseDTO createTaskInGroupWithHours(Long groupId, String title, Float estimatedHours, User createdBy) {
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
        User taskCreator = createdBy != null ? createdBy : group.getCreatedBy();

        Task task = new Task();
        task.setTodoList(targetList);
        task.setTitle(title);
        task.setDescription(title);
        task.setStatus(TaskStatus.pending);
        task.setPriority(TaskPriority.medium);
        task.setCreatedBy(taskCreator);
        task.setSprint(sprint);
        task.setEstimatedHours(estimatedHours != null ? estimatedHours : 1f);

        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO startTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(TaskStatus.in_progress);
        return mapToResponseDTO(taskRepository.save(task));
    }

    public TaskResponseDTO completeTask(Long taskId, Float actualHours) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(TaskStatus.completed);
        task.setActualHours(actualHours);
        return mapToResponseDTO(taskRepository.save(task));
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