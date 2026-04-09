package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
<<<<<<< HEAD
@Transactional
@EnableTransactionManagement
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmail(String email);

}

=======
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
>>>>>>> b26961ae249428dfecd7e76e23fb7aa83de8aa27
