package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskAssignment;
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
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TaskAssignmentTest {

	@Autowired
	private TaskAssignmentRepository taskAssignmentRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void saveAndFindByIdShouldWork() {
		User user = new User();
		user.setName("Laura Vega");
		user.setEmail("laura@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 7, 30));

		TaskGroup group = new TaskGroup();
		group.setName("Support Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 7, 45));

		TodoList list = new TodoList();
		list.setGroup(group);
		list.setName("Daily Tasks");
		list.setCreatedBy(user);
		list.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 0));

		Task task = new Task();
		task.setList(list);
		task.setTitle("Respond to tickets");
		task.setDescription("Handle the pending support tickets for the morning shift.");
		task.setStatus("IN_PROGRESS");
		task.setPriority("MEDIUM");
		task.setDueDate(LocalDateTime.of(2026, 4, 8, 17, 0));
		task.setCreatedBy(user);
		task.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 5));

		entityManager.persist(user);
		entityManager.persist(group);
		entityManager.persist(list);
		entityManager.persist(task);
		entityManager.flush();

		TaskAssignment assignment = new TaskAssignment();
		assignment.setTask(task);
		assignment.setUser(user);

		TaskAssignment savedAssignment = taskAssignmentRepository.saveAndFlush(assignment);
		entityManager.clear();

		Optional<TaskAssignment> foundAssignment = taskAssignmentRepository.findById(savedAssignment.getId());

		assertThat(foundAssignment).isPresent();
		assertThat(foundAssignment.get().getTask().getTitle()).isEqualTo("Respond to tickets");
		assertThat(foundAssignment.get().getUser().getEmail()).isEqualTo("laura@example.com");
	}

	@Test
	void shouldRejectDuplicateTaskAndUserCombination() {
		User user = new User();
		user.setName("Diego Torres");
		user.setEmail("diego@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));

		TaskGroup group = new TaskGroup();
		group.setName("QA Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 10));

		TodoList list = new TodoList();
		list.setGroup(group);
		list.setName("Regression Sprint");
		list.setCreatedBy(user);
		list.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 20));

		Task task = new Task();
		task.setList(list);
		task.setTitle("Run smoke tests");
		task.setDescription("Execute smoke tests before the release window.");
		task.setStatus("OPEN");
		task.setPriority("HIGH");
		task.setDueDate(LocalDateTime.of(2026, 4, 8, 19, 0));
		task.setCreatedBy(user);
		task.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 25));

		entityManager.persist(user);
		entityManager.persist(group);
		entityManager.persist(list);
		entityManager.persist(task);
		entityManager.flush();

		TaskAssignment firstAssignment = new TaskAssignment();
		firstAssignment.setTask(task);
		firstAssignment.setUser(user);
		taskAssignmentRepository.saveAndFlush(firstAssignment);

		TaskAssignment duplicateAssignment = new TaskAssignment();
		duplicateAssignment.setTask(task);
		duplicateAssignment.setUser(user);

		assertThrows(DataIntegrityViolationException.class,
			() -> taskAssignmentRepository.saveAndFlush(duplicateAssignment));
	}
}
