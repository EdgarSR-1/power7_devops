package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ToDoItemService {

    private final ToDoItemRepository repository;

    public ToDoItemService(ToDoItemRepository repository) {
        this.repository = repository;
    }

    public List<ToDoItem> findAll() {
        return repository.findAll();
    }

    public ResponseEntity<ToDoItem> getItemById(int id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    public ToDoItem getToDoItemById(int id) {
        return repository.findById(id).orElseThrow();
    }

    public ToDoItem addToDoItem(ToDoItem todoItem) {
        return repository.save(todoItem);
    }

    public ToDoItem updateToDoItem(int id, ToDoItem todoItem) {
        todoItem.setID(id);
        return repository.save(todoItem);
    }

    public boolean deleteToDoItem(int id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}