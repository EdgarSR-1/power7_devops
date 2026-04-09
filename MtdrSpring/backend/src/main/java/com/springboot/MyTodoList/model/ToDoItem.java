package com.springboot.MyTodoList.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "todoitem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToDoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ID;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "creation_ts")
    private OffsetDateTime creation_ts;
}