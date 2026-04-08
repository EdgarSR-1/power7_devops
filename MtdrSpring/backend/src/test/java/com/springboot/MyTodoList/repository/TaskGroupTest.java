package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.MyTodoList.model.TaskGroup;
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
class TaskGroupTest {

	@Autowired
	private TaskGroupRepository taskGroupRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void saveAndFindByIdShouldWork() {
		User user = new User();
		user.setName("Maria Garcia");
		user.setEmail("maria@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 8, 45));

		entityManager.persist(user);
		entityManager.flush();

		TaskGroup group = new TaskGroup();
		group.setName("QA Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));

		TaskGroup savedGroup = taskGroupRepository.saveAndFlush(group);
		entityManager.clear();

		Optional<TaskGroup> foundGroup = taskGroupRepository.findById(savedGroup.getId());

		assertThat(foundGroup).isPresent();
		assertThat(foundGroup.get().getName()).isEqualTo("QA Team");
		assertThat(foundGroup.get().getCreatedBy().getEmail()).isEqualTo("maria@example.com");
		assertThat(foundGroup.get().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 8, 9, 0));
	}
}
