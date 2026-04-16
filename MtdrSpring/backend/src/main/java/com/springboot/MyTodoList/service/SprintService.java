package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.SprintRequestDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SprintService {

    private static final Logger logger = LoggerFactory.getLogger(SprintService.class);
    private final SprintRepository sprintRepository;

    public SprintService(SprintRepository sprintRepository) {
        this.sprintRepository = sprintRepository;
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
        logger.info("DEBUG SprintService.createSprint: Validating DTO - name='{}', start={}, end={}", 
                dto.getName(), dto.getStartDate(), dto.getEndDate());
        
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            logger.error("DEBUG SprintService.createSprint: Sprint name is null or empty");
            throw new RuntimeException("Sprint name is required");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            logger.error("DEBUG SprintService.createSprint: Start or end date is null");
            throw new RuntimeException("Sprint start and end date are required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            logger.error("DEBUG SprintService.createSprint: End date before start date");
            throw new RuntimeException("Sprint end date cannot be before start date");
        }

        Sprint sprint = new Sprint();
        sprint.setName(dto.getName().trim());
        sprint.setStartDate(dto.getStartDate());
        sprint.setEndDate(dto.getEndDate());
        sprint.setGroupId(null);  // No group association for sprints
        
        logger.info("DEBUG SprintService.createSprint: About to persist Sprint entity: {}", sprint.getName());
        try {
            Sprint savedSprint = sprintRepository.save(sprint);
            logger.info("DEBUG SprintService.createSprint: Sprint saved successfully with id={}", savedSprint.getId());
            return savedSprint;
        } catch (Exception e) {
            logger.error("DEBUG SprintService.createSprint: Error persisting sprint - {}", e.getMessage(), e);
            throw new RuntimeException("Error saving sprint to database: " + e.getMessage(), e);
        }
    }
}