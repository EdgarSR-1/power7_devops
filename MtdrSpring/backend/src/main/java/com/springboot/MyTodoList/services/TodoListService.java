package com.springboot.MyTodoList.services;

import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.repository.TodoListRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoListService {

    private final TodoListRepository repository;

    public List<TodoList> findAll() {
        return repository.findAll();
    }

    public TodoList save(TodoList list) {
        return repository.save(list);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}