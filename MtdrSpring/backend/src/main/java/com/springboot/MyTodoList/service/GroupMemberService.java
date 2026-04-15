package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupMemberService {

    private final GroupMemberRepository repository;
    private final TaskGroupRepository taskGroupRepository;
    private final UserRepository userRepository;

    public GroupMemberService(
            GroupMemberRepository repository,
            TaskGroupRepository taskGroupRepository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.taskGroupRepository = taskGroupRepository;
        this.userRepository = userRepository;
    }

    public List<GroupMember> getAll() {
        return repository.findAll();
    }

    public GroupMember getById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RuntimeException("GroupMember not found with id: " + id));
    }

    public List<GroupMember> getByGroupId(Long groupId) {
        return repository.findByGroupId(groupId);
    }

    public List<GroupMember> getByUserId(Long userId) {
        return repository.findByUserId(userId);
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
}