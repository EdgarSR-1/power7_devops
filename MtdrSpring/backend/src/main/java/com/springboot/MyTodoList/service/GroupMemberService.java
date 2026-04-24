package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.RoleRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.model.RoleName;
import org.springframework.stereotype.Service;
import com.springboot.MyTodoList.dto.GroupMemberResponseDTO;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupMemberService {

    private final GroupMemberRepository repository;
    private final TaskGroupRepository taskGroupRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private GroupMemberResponseDTO mapToResponseDTO(GroupMember gm) {
    return new GroupMemberResponseDTO(
            gm.getId(),
            gm.getGroup() != null ? gm.getGroup().getId() : null,
            gm.getGroup() != null ? gm.getGroup().getName() : null,
            gm.getUser() != null ? gm.getUser().getId() : null,
            gm.getUser() != null ? gm.getUser().getName() : null,
            gm.getUser() != null ? gm.getUser().getEmail() : null,
            gm.getRole() != null ? gm.getRole().getId() : null,
            gm.getRole() != null && gm.getRole().getName() != null
                    ? gm.getRole().getName().name()
                    : gm.getRoleName(),
            gm.getJoinedAt()
    );
}

    public GroupMemberService(
            GroupMemberRepository repository,
            TaskGroupRepository taskGroupRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.repository = repository;
        this.taskGroupRepository = taskGroupRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<GroupMemberResponseDTO> getAll() {
    return repository.findAll()
            .stream()
            .map(this::mapToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    public GroupMemberResponseDTO getById(Long id) {
    GroupMember gm = repository.findById(id).orElseThrow(() ->
            new RuntimeException("GroupMember not found with id: " + id));

     return mapToResponseDTO(gm);
}

    public List<GroupMemberResponseDTO> getByGroupId(Long groupId) {
    return repository.findByGroupId(groupId)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<GroupMemberResponseDTO> getByUserId(Long userId) {
    return repository.findByUserId(userId)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    public GroupMember save(GroupMember groupMember) {
        if (groupMember.getGroup() == null || groupMember.getGroup().getId() == null) {
            throw new RuntimeException("Group ID is required");
        }

        if (groupMember.getUser() == null || groupMember.getUser().getId() == null) {
            throw new RuntimeException("User ID is required");
        }

        TaskGroup group = taskGroupRepository.findById(groupMember.getGroup().getId())
                .orElseThrow(() -> new RuntimeException("TaskGroup not found"));

        User user = userRepository.findById(groupMember.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        groupMember.setGroup(group);
        groupMember.setUser(user);
        if (groupMember.getRole() == null) {
            groupMember.setRole(resolveDefaultRole());
        }

        if (groupMember.getJoinedAt() == null) {
            groupMember.setJoinedAt(LocalDateTime.now());
        }

        return repository.save(groupMember);
    }

    public GroupMember update(Long id, GroupMember updatedGroupMember) {
        GroupMember existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("GroupMember not found"));

        if (updatedGroupMember.getGroup() != null && updatedGroupMember.getGroup().getId() != null) {
            TaskGroup group = taskGroupRepository.findById(updatedGroupMember.getGroup().getId())
                    .orElseThrow(() -> new RuntimeException("TaskGroup not found"));
            existing.setGroup(group);
        }

        if (updatedGroupMember.getUser() != null && updatedGroupMember.getUser().getId() != null) {
            User user = userRepository.findById(updatedGroupMember.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existing.setUser(user);
        }

        existing.setRole(updatedGroupMember.getRole());

        if (updatedGroupMember.getJoinedAt() != null) {
            existing.setJoinedAt(updatedGroupMember.getJoinedAt());
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private Role resolveDefaultRole() {
    return roleRepository.findByName(RoleName.USUARIO).orElseGet(() -> {
        Role role = new Role();
        role.setName(RoleName.USUARIO);
        role.setDescription("Default group member");
        return roleRepository.save(role);
    });
}
}
