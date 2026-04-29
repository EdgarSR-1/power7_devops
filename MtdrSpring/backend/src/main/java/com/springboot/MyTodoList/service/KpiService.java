package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.kpi.CompletedBySprintDTO;
import com.springboot.MyTodoList.dto.kpi.OverdueTaskDTO;
import com.springboot.MyTodoList.dto.kpi.StatusDistributionDTO;
import com.springboot.MyTodoList.dto.kpi.VelocityDTO;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.dto.kpi.CompletedTasksByUserSprintGroupDTO;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.List;

@Service
public class KpiService {

    private final TaskRepository taskRepository;

    public KpiService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<CompletedBySprintDTO> getCompletedTasksBySprint() {
        return taskRepository.getCompletedTasksBySprint();
    }

    public List<StatusDistributionDTO> getStatusDistribution() {
        return taskRepository.getStatusDistribution();
    }

    public List<OverdueTaskDTO> getOverdueTasks() {
        return taskRepository.getOverdueTasks();
    }

    public List<CompletedTasksByUserSprintGroupDTO> getCompletedTasksByUserSprintGroup(Long sprintId) {
    return taskRepository.getCompletedTasksByUserSprintGroupRaw(sprintId)
            .stream()
            .map(row -> new CompletedTasksByUserSprintGroupDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    (String) row[3],
                    ((Number) row[4]).longValue(),
                    (String) row[5],
                    ((Number) row[6]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public VelocityDTO getVelocity() {
        List<CompletedBySprintDTO> completedBySprint =
                taskRepository.getCompletedTasksBySprint();

        if (completedBySprint.isEmpty()) {
            return new VelocityDTO(0.0);
        }

        double average = completedBySprint.stream()
                .mapToLong(CompletedBySprintDTO::getCompletedTasks)
                .average()
                .orElse(0.0);

        return new VelocityDTO(average);
    }
}