package com.springboot.MyTodoList.services;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository repository;

    public GroupMemberService(GroupMemberRepository repository) {
        this.repository = repository;
    }

    public GroupMember save(GroupMember member) {
        return repository.save(member);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
