package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.kpi.CompletedBySprintDTO;
import com.springboot.MyTodoList.dto.kpi.OverdueTaskDTO;
import com.springboot.MyTodoList.dto.kpi.StatusDistributionDTO;
import com.springboot.MyTodoList.dto.kpi.VelocityDTO;
import com.springboot.MyTodoList.service.KpiService;
import com.springboot.MyTodoList.dto.kpi.CompletedTasksByUserSprintGroupDTO;
import com.springboot.MyTodoList.dto.kpi.HoursBySprintDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpis")
@CrossOrigin(origins = "http://localhost:3000")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/completed-by-sprint")
    public List<CompletedBySprintDTO> getCompletedTasksBySprint() {
        return kpiService.getCompletedTasksBySprint();
    }

    @GetMapping("/status-distribution")
    public List<StatusDistributionDTO> getStatusDistribution() {
        return kpiService.getStatusDistribution();
    }

    @GetMapping("/overdue-tasks")
    public List<OverdueTaskDTO> getOverdueTasks() {
        return kpiService.getOverdueTasks();
    }

    @GetMapping("/velocity")
    public VelocityDTO getVelocity() {
        return kpiService.getVelocity();
    }

    @GetMapping("/completed-by-user")
    public List<CompletedTasksByUserSprintGroupDTO> getCompletedTasksByUserSprintGroup(
        @RequestParam(required = false) Long sprintId
    ) {
    return kpiService.getCompletedTasksByUserSprintGroup(sprintId);
    }

    @GetMapping("/hours-by-sprint")
    public List<HoursBySprintDTO> getEstimatedHoursByUserSprintGroup(
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Long sprintId
    ) {
    return kpiService.getEstimatedHoursByUserSprintGroup(groupId, sprintId);
    }
}