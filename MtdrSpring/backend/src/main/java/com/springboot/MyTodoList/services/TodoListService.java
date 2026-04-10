package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.repository.TodoListRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoListService {

    private final TodoListRepository todoListRepository;

    public TodoListService(TodoListRepository todoListRepository) {
        this.todoListRepository = todoListRepository;
    }

    public TodoList createTodoList(TodoList todoList) {
        return todoListRepository.save(todoList);
    }

    public List<TodoList> getAllTodoLists() {
        return todoListRepository.findAll();
    }

    public TodoList getTodoListById(Long id) {
        return todoListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo list not found"));
    }

    public List<TodoList> getTodoListsByGroupId(Long groupId) {
        return todoListRepository.findByGroupId(groupId);
    }

    public TodoList updateTodoList(Long id, TodoList updatedTodoList) {
        TodoList existing = getTodoListById(id);

        existing.setName(updatedTodoList.getName());
        existing.setGroup(updatedTodoList.getGroup());
        existing.setCreatedBy(updatedTodoList.getCreatedBy());

        return todoListRepository.save(existing);
    }

    public void deleteTodoList(Long id) {
        todoListRepository.deleteById(id);
    }
}