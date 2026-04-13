package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.service.GroupMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-members")
@CrossOrigin(origins = "*")
public class GroupMemberController {

    private final GroupMemberService service;

    public GroupMemberController(GroupMemberService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GroupMember>> getAllGroupMembers() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupMember> getGroupMemberById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<GroupMember>> getByGroupId(@PathVariable Long groupId) {
        return ResponseEntity.ok(service.getByGroupId(groupId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupMember>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> createGroupMember(@RequestBody GroupMember groupMember) {
    try {
        GroupMember saved = service.save(groupMember);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
  }  

    @PutMapping("/{id}")
    public ResponseEntity<GroupMember> updateGroupMember(
            @PathVariable Long id,
            @RequestBody GroupMember groupMember) {
        try {
            GroupMember updated = service.update(id, groupMember);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupMember(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}