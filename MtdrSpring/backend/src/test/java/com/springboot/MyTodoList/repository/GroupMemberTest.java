package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.TaskGroup;
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
class GroupMemberTest {

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void saveAndFindByIdShouldWork() {
		User user = new User();
		user.setName("Ana Perez");
		user.setEmail("ana@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 15));

		TaskGroup group = new TaskGroup();
		group.setName("Product Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 30));

		entityManager.persist(user);
		entityManager.persist(group);
		entityManager.flush();

		GroupMember member = new GroupMember();
		member.setGroup(group);
		member.setUser(user);
		member.setRole("ADMIN");
		member.setJoinedAt(LocalDateTime.of(2026, 4, 8, 10, 0));

		GroupMember savedMember = groupMemberRepository.saveAndFlush(member);
		entityManager.clear();

		Optional<GroupMember> foundMember = groupMemberRepository.findById(savedMember.getId());

		assertThat(foundMember).isPresent();
		assertThat(foundMember.get().getGroup().getName()).isEqualTo("Product Team");
		assertThat(foundMember.get().getUser().getEmail()).isEqualTo("ana@example.com");
		assertThat(foundMember.get().getRole()).isEqualTo("ADMIN");
		assertThat(foundMember.get().getJoinedAt()).isEqualTo(LocalDateTime.of(2026, 4, 8, 10, 0));
	}

	@Test
	void shouldRejectDuplicateGroupAndUserCombination() {
		User user = new User();
		user.setName("Luis Gomez");
		user.setEmail("luis@example.com");
		user.setPassword("secret");
		user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 11, 0));

		TaskGroup group = new TaskGroup();
		group.setName("Backend Team");
		group.setCreatedBy(user);
		group.setCreatedAt(LocalDateTime.of(2026, 4, 8, 11, 5));

		entityManager.persist(user);
		entityManager.persist(group);
		entityManager.flush();

		GroupMember firstMember = new GroupMember();
		firstMember.setGroup(group);
		firstMember.setUser(user);
		firstMember.setRole("MEMBER");
		firstMember.setJoinedAt(LocalDateTime.of(2026, 4, 8, 11, 10));
		groupMemberRepository.saveAndFlush(firstMember);

		GroupMember duplicateMember = new GroupMember();
		duplicateMember.setGroup(group);
		duplicateMember.setUser(user);
		duplicateMember.setRole("ADMIN");
		duplicateMember.setJoinedAt(LocalDateTime.of(2026, 4, 8, 11, 15));

		assertThrows(DataIntegrityViolationException.class,
			() -> groupMemberRepository.saveAndFlush(duplicateMember));
	}
}
