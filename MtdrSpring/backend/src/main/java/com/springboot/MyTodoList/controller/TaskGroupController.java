package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.service.TaskGroupService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taskgroups")
@CrossOrigin(origins = "*")
public class TaskGroupController {

    private final TaskGroupService service;

    public TaskGroupController(TaskGroupService service) {
        this.service = service;
    }

    // GET all task groups
    @GetMapping
    public ResponseEntity<List<TaskGroup>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // GET task group by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskGroup> getById(@PathVariable Long id) {
        try {
            TaskGroup group = service.findById(id);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST create task group (manual user)
    @PostMapping
    public ResponseEntity<TaskGroup> create(@RequestBody TaskGroup group) {
        TaskGroup saved = service.save(group);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // POST create task group using bot owner
    @PostMapping("/bot")
    public ResponseEntity<TaskGroup> createWithBot(@RequestParam String name) {
        TaskGroup group = service.createGroupForBot(name);
        return new ResponseEntity<>(group, HttpStatus.CREATED);
    }

    // DELETE task group
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}