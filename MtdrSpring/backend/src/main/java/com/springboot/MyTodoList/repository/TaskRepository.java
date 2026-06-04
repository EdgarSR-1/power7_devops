package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.springboot.MyTodoList.dto.kpi.CompletedBySprintDTO;
import com.springboot.MyTodoList.dto.kpi.OverdueTaskDTO;
import com.springboot.MyTodoList.model.TaskPriority;
import com.springboot.MyTodoList.model.TaskStatus;
import org.springframework.data.domain.Pageable;
import com.springboot.MyTodoList.dto.kpi.StatusDistributionDTO;
import com.springboot.MyTodoList.dto.kpi.CompletedTasksByUserSprintGroupDTO;
import com.springboot.MyTodoList.dto.kpi.HoursBySprintDTO;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTodoListId(Long listId);
    List<Task> findByCreatedById(Long userId);
    List<Task> findByTodoListGroupId(Long groupId);
    List<Task> findBySprintIdOrderByCreatedAtAsc(Long sprintId);

    // KPIs
    @Query(
    "SELECT new com.springboot.MyTodoList.dto.kpi.StatusDistributionDTO(" +
    "t.status, COUNT(t)) " +
    "FROM Task t " +
    "GROUP BY t.status"
    )
    List<StatusDistributionDTO> getStatusDistribution();

    @Query(
    "SELECT new com.springboot.MyTodoList.dto.kpi.CompletedBySprintDTO(" +
    "s.id, s.name, COUNT(t)) " +
    "FROM Task t " +
    "JOIN t.sprint s " +
    "WHERE t.status = com.springboot.MyTodoList.model.TaskStatus.completed " +
    "GROUP BY s.id, s.name " +
    "ORDER BY s.id"
    )
    List<CompletedBySprintDTO> getCompletedTasksBySprint();

    @Query(
    "SELECT new com.springboot.MyTodoList.dto.kpi.OverdueTaskDTO(" +
    "t.id, t.title, t.status, t.dueDate) " +
    "FROM Task t " +
    "WHERE t.dueDate < CURRENT_TIMESTAMP " +
    "AND t.status <> com.springboot.MyTodoList.model.TaskStatus.completed " +
    "ORDER BY t.dueDate ASC"
    )
    List<OverdueTaskDTO> getOverdueTasks();

    @Query(value =
    "SELECT " +
    "tg.id AS groupId, " +
    "tg.name AS groupName, " +
    "s.id AS sprintId, " +
    "s.name AS sprintName, " +
    "u.id AS userId, " +
    "u.name AS userName, " +
    "COUNT(t.id) AS completedTasks " +
    "FROM tasks t " +
    "JOIN task_assignments ta ON ta.task_id = t.id " +
    "JOIN users u ON u.id = ta.user_id " +
    "JOIN todo_lists tl ON tl.id = t.list_id " +
    "JOIN taskgroups tg ON tg.id = tl.group_id " +
    "JOIN sprints s ON s.id = t.sprint_id " +
    "WHERE t.status = 'completed' " +
    "AND (:sprintId IS NULL OR s.id = :sprintId) " +
    "GROUP BY tg.id, tg.name, s.id, s.name, u.id, u.name " +
    "ORDER BY tg.name, s.id, completedTasks DESC",
    nativeQuery = true
    )
    List<Object[]> getCompletedTasksByUserSprintGroupRaw(@Param("sprintId") Long sprintId);

    @Query(value =
    "SELECT " +
    "tg.id AS groupId, " +
    "tg.name AS groupName, " +
    "s.id AS sprintId, " +
    "s.name AS sprintName, " +
    "u.id AS userId, " +
    "u.name AS userName, " +
    "COALESCE(SUM(t.estimated_hours), 0) AS estimatedHours " +
    "FROM tasks t " +
    "JOIN task_assignments ta ON ta.task_id = t.id " +
    "JOIN users u ON u.id = ta.user_id " +
    "JOIN todo_lists tl ON tl.id = t.list_id " +
    "JOIN taskgroups tg ON tg.id = tl.group_id " +
    "JOIN sprints s ON s.id = t.sprint_id " +
    "WHERE (:groupId IS NULL OR tg.id = :groupId) " +
    "AND (:sprintId IS NULL OR s.id = :sprintId) " +
    "GROUP BY tg.id, tg.name, s.id, s.name, u.id, u.name " +
    "ORDER BY tg.name, s.id, estimatedHours DESC",
    nativeQuery = true
    )
    List<Object[]> getEstimatedHoursByUserSprintGroupRaw(
        @Param("groupId") Long groupId,
        @Param("sprintId") Long sprintId
    );

        @Query("SELECT t FROM Task t " +
            "WHERE t.dueDate IS NOT NULL " +
            "AND t.dueDate < :now " +
            "AND t.status <> :completedStatus " +
            "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasksForSuperAdmin(
            @Param("now") LocalDateTime now,
            @Param("completedStatus") TaskStatus completedStatus,
            Pageable pageable
    );

   @Query("SELECT t FROM Task t " +
        "WHERE t.todoList IS NOT NULL " +
        "AND t.todoList.group IS NOT NULL " +
        "AND t.dueDate IS NOT NULL " +
        "AND t.dueDate < :now " +
        "AND t.status <> :completedStatus " +
        "AND EXISTS (" +
        "   SELECT gm.id FROM GroupMember gm " +
        "   WHERE gm.group.id = t.todoList.group.id " +
        "   AND gm.user.id = :userId" +
        ") " +
        "ORDER BY t.dueDate ASC")
        List<Task> findOverdueTasksVisibleToUser(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        @Param("completedStatus") TaskStatus completedStatus,
        Pageable pageable
        );

    @Query("SELECT t FROM Task t " +
            "WHERE t.status = :status " +
            "ORDER BY t.createdAt DESC")
    List<Task> findTasksByStatusForSuperAdmin(
            @Param("status") TaskStatus status,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t " +
        "WHERE t.todoList IS NOT NULL " +
        "AND t.todoList.group IS NOT NULL " +
        "AND t.status = :status " +
        "AND EXISTS (" +
        "   SELECT gm.id FROM GroupMember gm " +
        "   WHERE gm.group.id = t.todoList.group.id " +
        "   AND gm.user.id = :userId" +
        ") " +
        "ORDER BY t.createdAt DESC")
        List<Task> findTasksByStatusVisibleToUser(
        @Param("userId") Long userId,
        @Param("status") TaskStatus status,
        Pageable pageable
        );

    @Query("SELECT t FROM Task t " +
        "WHERE t.priority = :priority " +
        "AND t.status <> :completedStatus " +
        "ORDER BY t.dueDate ASC")
List<Task> findTasksByPriorityForSuperAdmin(
        @Param("priority") TaskPriority priority,
        @Param("completedStatus") TaskStatus completedStatus,
        Pageable pageable
);

    @Query("SELECT t FROM Task t " +
        "WHERE t.todoList IS NOT NULL " +
        "AND t.todoList.group IS NOT NULL " +
        "AND t.priority = :priority " +
        "AND t.status <> :completedStatus " +
        "AND EXISTS (" +
        "   SELECT gm.id FROM GroupMember gm " +
        "   WHERE gm.group.id = t.todoList.group.id " +
        "   AND gm.user.id = :userId" +
        ") " +
        "ORDER BY t.dueDate ASC")
        List<Task> findTasksByPriorityVisibleToUser(
        @Param("userId") Long userId,
        @Param("priority") TaskPriority priority,
        @Param("completedStatus") TaskStatus completedStatus,
        Pageable pageable
        );       

    @Query("SELECT t FROM Task t " +
            "WHERE t.sprint.id = :sprintId " +
            "ORDER BY t.createdAt DESC")
    List<Task> findTasksBySprintForSuperAdmin(
            @Param("sprintId") Long sprintId,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t " +
        "WHERE t.todoList IS NOT NULL " +
        "AND t.todoList.group IS NOT NULL " +
        "AND t.sprint.id = :sprintId " +
        "AND EXISTS (" +
        "   SELECT gm.id FROM GroupMember gm " +
        "   WHERE gm.group.id = t.todoList.group.id " +
        "   AND gm.user.id = :userId" +
        ") " +
        "ORDER BY t.createdAt DESC")
List<Task> findTasksBySprintVisibleToUser(
        @Param("userId") Long userId,
        @Param("sprintId") Long sprintId,
        Pageable pageable
);

}