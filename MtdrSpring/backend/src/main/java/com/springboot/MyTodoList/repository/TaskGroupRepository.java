package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.TaskGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {
	List<TaskGroup> findByCreatedById(Long userId);
  Optional<TaskGroup> findFirstByOrderByIdAsc();
}