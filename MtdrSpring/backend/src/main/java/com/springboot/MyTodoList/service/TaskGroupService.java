package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.CreateGroupTaskRequestDTO;
import com.springboot.MyTodoList.dto.GroupMemberDTO;
import com.springboot.MyTodoList.dto.GroupTaskDTO;
import com.springboot.MyTodoList.dto.TaskGroupDetailDTO;
import com.springboot.MyTodoList.dto.TaskGroupRequestDTO;
import com.springboot.MyTodoList.dto.TaskGroupSummaryDTO;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.model.TaskPriority;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.RoleRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGroupService {

    private final TaskGroupRepository repository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RoleRepository roleRepository;
    private final TaskRepository taskRepository;
    private final TodoListRepository todoListRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskGroupService(TaskGroupRepository repository,
                            UserRepository userRepository,
                            GroupMemberRepository groupMemberRepository,
                            RoleRepository roleRepository,
                            TaskRepository taskRepository,
                            TodoListRepository todoListRepository,
                            TaskAssignmentRepository taskAssignmentRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.roleRepository = roleRepository;
        this.taskRepository = taskRepository;
        this.todoListRepository = todoListRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public List<TaskGroup> findAll() {
        return repository.findAll();
    }

    public TaskGroup save(TaskGroup group) {
        return repository.save(group);
    }

    @Transactional
    public TaskGroupSummaryDTO createGroup(TaskGroupRequestDTO request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }

        if (request.getCreatedById() == null) {
            throw new IllegalArgumentException("createdById is required");
        }

        User createdBy = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found"));

        TaskGroup group = new TaskGroup();
        group.setName(request.getName().trim());
        group.setDescription(normalizeOptionalText(request.getDescription()));
        group.setCreatedBy(createdBy);

        TaskGroup savedGroup = repository.save(group);
        addMembers(savedGroup, request);

        return toSummaryDTO(savedGroup);
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

    public List<TaskGroupSummaryDTO> getSummaries() {
        return repository.findAll()
                .stream()
                .sorted(Comparator.comparing(TaskGroup::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    public TaskGroupDetailDTO getDetail(Long id) {
        TaskGroup group = findById(id);
        TaskGroupSummaryDTO summary = toSummaryDTO(group);
        List<GroupTaskDTO> tasks = taskRepository.findByTodoListGroupId(group.getId())
                .stream()
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTaskDTO)
                .collect(Collectors.toList());

        return new TaskGroupDetailDTO(
                summary.getId(),
                summary.getName(),
                summary.getDescription(),
                summary.getCreatedAt(),
                summary.getCreatedBy(),
                summary.getCompletedTasks(),
                summary.getTotalTasks(),
                summary.getMembers(),
                tasks
        );
    }

    @Transactional
    public GroupTaskDTO createTask(Long groupId, CreateGroupTaskRequestDTO request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required");
        }

        if (request.getAssigneeId() == null) {
            throw new IllegalArgumentException("assigneeId is required");
        }

        TaskGroup group = findById(groupId);
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserId(groupId, request.getAssigneeId())
                .orElseThrow(() -> new IllegalArgumentException("Assignee is not a member of this group"));

        TodoList targetList = todoListRepository.findByGroupId(groupId)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    TodoList createdList = new TodoList();
                    createdList.setGroup(group);
                    createdList.setName("General");
                    createdList.setCreatedBy(group.getCreatedBy());
                    return todoListRepository.save(createdList);
                });

        Task task = new Task();
        task.setTodoList(targetList);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getTitle().trim());
        task.setStatus(TaskStatus.pending);
        task.setPriority(TaskPriority.medium);
        task.setDueDate(request.getDueDate());
        task.setCreatedBy(group.getCreatedBy());

        Task savedTask = taskRepository.save(task);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(savedTask);
        assignment.setUser(groupMember.getUser());
        taskAssignmentRepository.save(assignment);

        return toTaskDTO(savedTask);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private TaskGroupSummaryDTO toSummaryDTO(TaskGroup group) {
        List<Task> tasks = taskRepository.findByTodoListGroupId(group.getId());
        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.completed)
                .count();

        return new TaskGroupSummaryDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedAt(),
                toMemberDTO(group.getCreatedBy(), null),
                completedTasks,
                tasks.size(),
                getGroupMembers(group.getId())
        );
    }

    private List<GroupMemberDTO> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId)
                .stream()
                .map(this::toGroupMemberDTO)
                .collect(Collectors.toList());
    }

    private GroupTaskDTO toTaskDTO(Task task) {
        List<GroupMemberDTO> assignees = taskAssignmentRepository.findByTaskId(task.getId())
                .stream()
                .map(TaskAssignment::getUser)
                .map(user -> toMemberDTO(user, null))
                .collect(Collectors.toList());

        return new GroupTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getStatus() != null ? task.getStatus().name() : null,
                task.getDueDate(),
                assignees
        );
    }

    private GroupMemberDTO toGroupMemberDTO(GroupMember groupMember) {
        Role role = groupMember.getRole();
        String roleName = groupMember.getRoleName();
        if ((roleName == null || roleName.isBlank()) && role != null) {
            roleName = role.getName();
        }

        return toMemberDTO(groupMember.getUser(), roleName);
    }

    private GroupMemberDTO toMemberDTO(User user, String role) {
        if (user == null) {
            return null;
        }

        return new GroupMemberDTO(user.getId(), user.getName(), role);
    }

    private void addMembers(TaskGroup group, TaskGroupRequestDTO request) {
        Role defaultRole = roleRepository.findByNameIgnoreCase("member")
                .or(() -> roleRepository.findFirstByOrderByIdAsc())
                .orElseThrow(() -> new IllegalArgumentException("No roles found to assign group members"));

        Set<Long> memberIds = new LinkedHashSet<>();
        memberIds.add(request.getCreatedById());

        if (request.getMemberIds() != null) {
            request.getMemberIds()
                    .stream()
                    .filter(id -> id != null)
                    .forEach(memberIds::add);
        }

        for (Long memberId : memberIds) {
            User user = userRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Member user not found: " + memberId));

            GroupMember groupMember = new GroupMember();
            groupMember.setGroup(group);
            groupMember.setUser(user);
            groupMember.setRole(defaultRole);
            groupMemberRepository.save(groupMember);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
