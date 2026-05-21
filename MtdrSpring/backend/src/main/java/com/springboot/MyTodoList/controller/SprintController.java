package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.SprintRequestDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.service.SprintService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@CrossOrigin(origins = "http://localhost:3000")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @GetMapping
    public ResponseEntity<List<Sprint>> getAll() {
        return ResponseEntity.ok(sprintService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sprint> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sprintService.findById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/current")
    public ResponseEntity<Sprint> getCurrent() {
        try {
            return ResponseEntity.ok(sprintService.getCurrentSprint());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Sprint> create(@RequestBody SprintRequestDTO dto) {
        Sprint sprint = sprintService.createSprint(dto);
        return new ResponseEntity<>(sprint, HttpStatus.CREATED);
    }
}