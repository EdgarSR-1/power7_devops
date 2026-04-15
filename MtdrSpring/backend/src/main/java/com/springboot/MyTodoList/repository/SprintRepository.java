package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Optional<Sprint> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDateTime taskStartDate,
            LocalDateTime taskEndDate
    );
}