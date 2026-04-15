package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.service.TodoListService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todolists")
@CrossOrigin
public class TodoListController {

    private final TodoListService todoListService;

    public TodoListController(TodoListService todoListService) {
        this.todoListService = todoListService;
    }

    @PostMapping
    public TodoList createTodoList(@RequestBody TodoList todoList) {
        return todoListService.createTodoList(todoList);
    }

    @GetMapping
    public List<TodoList> getAllTodoLists() {
        return todoListService.getAllTodoLists();
    }

    @GetMapping("/{id}")
    public TodoList getTodoListById(@PathVariable Long id) {
        return todoListService.getTodoListById(id);
    }

    @GetMapping("/group/{groupId}")
    public List<TodoList> getTodoListsByGroupId(@PathVariable Long groupId) {
        return todoListService.getTodoListsByGroupId(groupId);
    }

    @PutMapping("/{id}")
    public TodoList updateTodoList(@PathVariable Long id, @RequestBody TodoList todoList) {
        return todoListService.updateTodoList(id, todoList);
    }

    @DeleteMapping("/{id}")
    public void deleteTodoList(@PathVariable Long id) {
        todoListService.deleteTodoList(id);
    }
}