package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.SprintRequestDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskGroupRepository taskGroupRepository;

    public SprintService(SprintRepository sprintRepository, TaskGroupRepository taskGroupRepository) {
        this.sprintRepository = sprintRepository;
        this.taskGroupRepository = taskGroupRepository;
    }

    public List<Sprint> findAll() {
        return sprintRepository.findAll();
    }

    public Sprint findById(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));
    }

    public Sprint getCurrentSprint() {
        LocalDateTime now = LocalDateTime.now();
        return sprintRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(now, now)
                .orElseThrow(() -> new RuntimeException("No active sprint found for current date/time."));
    }

    public Sprint createSprint(SprintRequestDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Sprint name is required");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new RuntimeException("Sprint start and end date are required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("Sprint end date cannot be before start date");
        }

        Sprint sprint = new Sprint();
        sprint.setName(dto.getName().trim());
        sprint.setStartDate(dto.getStartDate());
        sprint.setEndDate(dto.getEndDate());

        Long resolvedGroupId = dto.getGroupId();
        if (resolvedGroupId != null) {
            TaskGroup group = taskGroupRepository.findById(resolvedGroupId)
                    .orElseThrow(() -> new RuntimeException("Group not found: " + resolvedGroupId));
            sprint.setGroupId(group.getId());
        } else {
            Long defaultGroupId = taskGroupRepository.findFirstByOrderByIdAsc()
                    .map(TaskGroup::getId)
                    .orElseThrow(() -> new RuntimeException("No task groups found. Create a group before creating sprints, or pass groupId."));
            sprint.setGroupId(defaultGroupId);
        }
        
        return sprintRepository.save(sprint);
    }
}