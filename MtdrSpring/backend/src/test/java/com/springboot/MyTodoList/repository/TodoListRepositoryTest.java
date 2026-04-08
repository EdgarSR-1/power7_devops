package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TodoListRepositoryTest {

    @Autowired
    private TodoListRepository todoListRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndLoadWithRelationsShouldWork() {
        User creator = new User();
        creator.setName("Mario Lopez");
        creator.setEmail("mario.todo@example.com");
        creator.setPassword("secret");
        creator.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
        User savedCreator = entityManager.persistAndFlush(creator);

        TaskGroup group = new TaskGroup();
        group.setName("Personal");
        group.setCreatedBy(savedCreator);
        group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 15));
        TaskGroup savedGroup = entityManager.persistAndFlush(group);

        TodoList todoList = new TodoList();
        todoList.setName("Pendientes de abril");
        todoList.setCreatedBy(savedCreator);
        todoList.setGroup(savedGroup);
        todoList.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 30));

        TodoList savedTodoList = entityManager.persistAndFlush(todoList);
        entityManager.clear();

        Optional<TodoList> foundTodoList = todoListRepository.findById(savedTodoList.getId());

        assertThat(foundTodoList).isPresent();
        assertThat(foundTodoList.get().getId()).isEqualTo(savedTodoList.getId());
        assertThat(foundTodoList.get().getName()).isEqualTo("Pendientes de abril");
        assertThat(foundTodoList.get().getCreatedBy().getEmail()).isEqualTo("mario.todo@example.com");
        assertThat(foundTodoList.get().getGroup().getName()).isEqualTo("Personal");
        assertThat(foundTodoList.get().getGroup().getCreatedBy().getEmail()).isEqualTo("mario.todo@example.com");
    }
}
