package com.springboot.MyTodoList.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DebugController {

    private final JdbcTemplate jdbcTemplate;

    public DebugController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/debug/db")
    public Map<String, Object> debugDb() {
        return jdbcTemplate.queryForMap(
            "select sys_context('USERENV','SESSION_USER') as USERNAME from dual"
        );
    }
}