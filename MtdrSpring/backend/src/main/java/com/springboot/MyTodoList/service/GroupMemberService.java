package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupMemberService {

    private final GroupMemberRepository repository;

    public GroupMemberService(GroupMemberRepository repository) {
        this.repository = repository;
    }

    public List<GroupMember> getAll() {
        return repository.findAll();
    }
}