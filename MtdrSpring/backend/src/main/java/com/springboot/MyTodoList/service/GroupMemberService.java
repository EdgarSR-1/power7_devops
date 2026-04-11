package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository repository;

    public List<GroupMember> getAll() {
        return repository.findAll();
    }
}