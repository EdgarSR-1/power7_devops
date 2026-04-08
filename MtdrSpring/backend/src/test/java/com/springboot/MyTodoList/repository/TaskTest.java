package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.MyTodoList.model.Task;
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
class TaskTest {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void saveAndFindByIdShouldWork() {
		User user = new User();
		user.setName("Carlos Ruiz");
		user.setEmail("carlos@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 0));

		TaskGroup group = new TaskGroup();
		group.setName("Operations Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 15));

		TodoList list = new TodoList();
		list.setGroup(group);
		list.setName("Sprint Backlog");
		list.setCreatedBy(user);
		list.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 30));

		entityManager.persist(user);
		entityManager.persist(group);
		entityManager.persist(list);
		entityManager.flush();

		Task task = new Task();
		task.setList(list);
		task.setTitle("Prepare release notes");
		task.setDescription("Draft and review the release notes before deployment.");
		task.setStatus("OPEN");
		task.setPriority("HIGH");
		task.setDueDate(LocalDateTime.of(2026, 4, 9, 18, 0));
		task.setCreatedBy(user);
		task.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));

		Task savedTask = taskRepository.saveAndFlush(task);
		entityManager.clear();

		Optional<Task> foundTask = taskRepository.findById(savedTask.getId());

		assertThat(foundTask).isPresent();
		assertThat(foundTask.get().getTitle()).isEqualTo("Prepare release notes");
		assertThat(foundTask.get().getDescription()).contains("release notes");
		assertThat(foundTask.get().getStatus()).isEqualTo("OPEN");
		assertThat(foundTask.get().getPriority()).isEqualTo("HIGH");
		assertThat(foundTask.get().getDueDate()).isEqualTo(LocalDateTime.of(2026, 4, 9, 18, 0));
		assertThat(foundTask.get().getList().getName()).isEqualTo("Sprint Backlog");
		assertThat(foundTask.get().getCreatedBy().getEmail()).isEqualTo("carlos@example.com");
	}
}
