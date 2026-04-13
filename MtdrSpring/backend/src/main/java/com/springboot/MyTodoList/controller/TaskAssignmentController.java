package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.service.TaskAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-assignments")
@CrossOrigin(origins = "*")
public class TaskAssignmentController {

    private final TaskAssignmentService service;

    public TaskAssignmentController(TaskAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TaskAssignment>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskAssignment>> getByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTaskId(taskId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskAssignment>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaskAssignment taskAssignment) {
        try {
            TaskAssignment saved = service.save(taskAssignment);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TaskAssignment taskAssignment) {
        try {
            TaskAssignment updated = service.update(id, taskAssignment);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}