/* package com.springboot.MyTodoList.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.MyTodoList.model.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindByEmailShouldWork() {
        User user = new User();
        user.setName("Mario Lopez");
        user.setEmail("mario@example.com");
        user.setPassword("secret");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 30));

        User savedUser = entityManager.persistAndFlush(user);
        entityManager.clear();

        Optional<User> foundUser = userRepository.findByEmail("mario@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getName()).isEqualTo("Mario Lopez");
        assertThat(foundUser.get().getEmail()).isEqualTo("mario@example.com");
        assertThat(foundUser.get().getPassword()).isEqualTo("secret");
    }
}

*/