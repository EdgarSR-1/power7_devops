package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.MyTodoList.model.ToDoItem;
import java.time.OffsetDateTime;
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
class ToDoItemRepositoryTest {

    @Autowired
    private ToDoItemRepository toDoItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindByIdShouldWork() {
        ToDoItem item = new ToDoItem();
        item.setDescription("Comprar leche");
        item.setDone(false);
        item.setCreation_ts(OffsetDateTime.parse("2026-04-08T11:00:00Z"));

        ToDoItem savedItem = entityManager.persistAndFlush(item);
        entityManager.clear();

        Optional<ToDoItem> foundItem = toDoItemRepository.findById(savedItem.getID());

        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getID()).isEqualTo(savedItem.getID());
        assertThat(foundItem.get().getDescription()).isEqualTo("Comprar leche");
        assertThat(foundItem.get().isDone()).isFalse();
        assertThat(foundItem.get().getCreation_ts()).isEqualTo(OffsetDateTime.parse("2026-04-08T11:00:00Z"));
    }
}
