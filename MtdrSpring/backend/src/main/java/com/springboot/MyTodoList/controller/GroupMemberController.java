package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.GroupMemberResponseDTO;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.service.GroupMemberService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/group-members")
@CrossOrigin(origins = "http://localhost:3000")
public class GroupMemberController {

    private final GroupMemberService service;
    private final UserRepository userRepository;

    public GroupMemberController(
            GroupMemberService service,
            UserRepository userRepository
    ) {
        this.service = service;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<List<GroupMemberResponseDTO>> getMyGroupMembers(
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(service.getByUserId(currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<GroupMemberResponseDTO>> getAllGroupMembers() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupMemberResponseDTO> getGroupMemberById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<GroupMemberResponseDTO>> getByGroupId(@PathVariable Long groupId) {
        return ResponseEntity.ok(service.getByGroupId(groupId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupMemberResponseDTO>> getByUserId(@PathVariable Long userId) {
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
            @RequestBody GroupMember groupMember
    ) {
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