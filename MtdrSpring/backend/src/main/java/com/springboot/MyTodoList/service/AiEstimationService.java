package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.ai.AiEstimateRequestDTO;
import com.springboot.MyTodoList.dto.ai.AiEstimateResponseDTO;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.RoleName;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiEstimationService {

    private static final Logger logger = LoggerFactory.getLogger(AiEstimationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_TASKS_IN_PROMPT = 45;
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("(?:tarea|task)\\s*#?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPRINT_ID_PATTERN = Pattern.compile("sprint\\s*#?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HASH_ID_PATTERN = Pattern.compile("#(\\d+)");

    private final DeepSeekService deepSeekService;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public AiEstimationService(
            DeepSeekService deepSeekService,
            TaskRepository taskRepository,
            SprintRepository sprintRepository,
            GroupMemberRepository groupMemberRepository,
            TaskAssignmentRepository taskAssignmentRepository) {
        this.deepSeekService = deepSeekService;
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public AiEstimateResponseDTO answerQuestion(AiEstimateRequestDTO request, User currentUser) {
        String question = request != null && request.getQuestion() != null
                ? request.getQuestion().trim()
                : "";

        if (question.isEmpty()) {
            return new AiEstimateResponseDTO(
                    "Escribe una pregunta, por ejemplo: /estimate Cuantas horas tomara la tarea #12? o Como va la carga del sprint 3?",
                    false
            );
        }

        Long requestedTaskId = request != null && request.getTaskId() != null
                ? request.getTaskId()
                : extractTaskId(question);
        Long requestedSprintId = request != null && request.getSprintId() != null
                ? request.getSprintId()
                : extractSprintId(question);
        Long requestedGroupId = request != null ? request.getGroupId() : null;

        List<Task> visibleTasks = filterByGroup(getVisibleTasks(currentUser), requestedGroupId);
        String fallbackAnswer = buildFallbackAnswer(question, visibleTasks, requestedTaskId, requestedSprintId);

        if (!deepSeekService.isConfigured()) {
            return new AiEstimateResponseDTO(fallbackAnswer, false);
        }

        String prompt = buildPrompt(question, visibleTasks, requestedTaskId, requestedSprintId, requestedGroupId, fallbackAnswer);

        try {
            String generatedAnswer = deepSeekService.generateText(prompt);
            if (isUsableAiAnswer(generatedAnswer, prompt)) {
                return new AiEstimateResponseDTO(generatedAnswer.trim(), true);
            }
        } catch (Exception ex) {
            logger.warn("AI estimation failed, using local KPI fallback", ex);
        }

        return new AiEstimateResponseDTO(fallbackAnswer, false);
    }

    public String answerQuestion(String question, User currentUser) {
        return answerQuestion(new AiEstimateRequestDTO(question), currentUser).getAnswer();
    }

    private boolean isUsableAiAnswer(String generatedAnswer, String prompt) {
        return generatedAnswer != null
                && !generatedAnswer.isBlank()
                && !generatedAnswer.trim().equals(prompt.trim());
    }

    private List<Task> getVisibleTasks(User currentUser) {
        List<Task> allTasks = taskRepository.findAll();
        if (currentUser == null || currentUser.getId() == null || isSuperAdmin(currentUser)) {
            return allTasks;
        }

        List<GroupMember> memberships = groupMemberRepository.findByUserId(currentUser.getId());
        Set<Long> groupIds = memberships.stream()
                .map(groupMember -> groupMember.getGroup() != null ? groupMember.getGroup().getId() : null)
                .filter(groupId -> groupId != null)
                .collect(Collectors.toCollection(HashSet::new));

        return allTasks.stream()
                .filter(task -> {
                    Long groupId = extractGroupId(task);
                    return groupId != null && groupIds.contains(groupId);
                })
                .collect(Collectors.toList());
    }

    private List<Task> filterByGroup(List<Task> tasks, Long groupId) {
        if (groupId == null) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> groupId.equals(extractGroupId(task)))
                .collect(Collectors.toList());
    }

    private boolean isSuperAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.SUPERADMIN;
    }

    private Long extractGroupId(Task task) {
        if (task == null || task.getTodoList() == null || task.getTodoList().getGroup() == null) {
            return null;
        }
        return task.getTodoList().getGroup().getId();
    }

    private Long extractTaskId(String question) {
        Matcher taskMatcher = TASK_ID_PATTERN.matcher(question);
        if (taskMatcher.find()) {
            return parseLong(taskMatcher.group(1));
        }

        Matcher hashMatcher = HASH_ID_PATTERN.matcher(question);
        if (hashMatcher.find() && !question.toLowerCase(Locale.ROOT).contains("sprint")) {
            return parseLong(hashMatcher.group(1));
        }

        return null;
    }

    private Long extractSprintId(String question) {
        Matcher sprintMatcher = SPRINT_ID_PATTERN.matcher(question);
        if (sprintMatcher.find()) {
            return parseLong(sprintMatcher.group(1));
        }
        return null;
    }

    private Long parseLong(String rawValue) {
        try {
            return Long.valueOf(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildPrompt(
            String question,
            List<Task> visibleTasks,
            Long requestedTaskId,
            Long requestedSprintId,
            Long requestedGroupId,
            String fallbackAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un asistente de gestion agil. Responde en espanol claro y breve.\n")
                .append("Tu tarea es estimar horas de tareas o explicar la carga de horas de un sprint usando solo los KPIs y tareas visibles.\n")
                .append("Si faltan datos, di que es una estimacion y explica la base KPI usada. No inventes ids ni tareas.\n\n")
                .append("Pregunta del usuario:\n")
                .append(question)
                .append("\n\nFiltros detectados:\n")
                .append("taskId=").append(requestedTaskId != null ? requestedTaskId : "-")
                .append(", sprintId=").append(requestedSprintId != null ? requestedSprintId : "-")
                .append(", groupId=").append(requestedGroupId != null ? requestedGroupId : "-")
                .append("\n\nKPIs visibles:\n")
                .append(buildKpiContext(visibleTasks))
                .append("\n\nTareas visibles mas relevantes:\n")
                .append(buildTaskContext(visibleTasks, requestedTaskId, requestedSprintId))
                .append("\n\nRespuesta base calculada por el sistema:\n")
                .append(fallbackAnswer)
                .append("\n\nResponde con una recomendacion accionable de 3 a 7 lineas.");
        return prompt.toString();
    }

    private String buildKpiContext(List<Task> tasks) {
        Metrics metrics = calculateMetrics(tasks);
        StringBuilder context = new StringBuilder();
        context.append("- Total tareas: ").append(tasks.size()).append("\n")
                .append("- Pendientes: ").append(metrics.pendingCount)
                .append(", en progreso: ").append(metrics.inProgressCount)
                .append(", completadas: ").append(metrics.completedCount).append("\n")
                .append("- Horas estimadas totales: ").append(formatHours(metrics.totalEstimatedHours)).append("\n")
                .append("- Horas estimadas restantes: ").append(formatHours(metrics.remainingEstimatedHours)).append("\n")
                .append("- Horas reales registradas: ").append(formatHours(metrics.totalActualHours)).append("\n")
                .append("- Promedio KPI por tarea: ").append(formatHours(calculateRecommendedAverage(tasks))).append("\n");

        Map<String, Metrics> sprintMetrics = groupMetricsBySprint(tasks);
        if (!sprintMetrics.isEmpty()) {
            context.append("- Horas por sprint:\n");
            for (Map.Entry<String, Metrics> entry : sprintMetrics.entrySet()) {
                Metrics sprint = entry.getValue();
                context.append("  * ").append(entry.getKey())
                        .append(": estimadas ").append(formatHours(sprint.totalEstimatedHours))
                        .append(", restantes ").append(formatHours(sprint.remainingEstimatedHours))
                        .append(", reales ").append(formatHours(sprint.totalActualHours))
                        .append(", tareas ").append(sprint.taskCount)
                        .append("\n");
            }
        }

        return context.toString();
    }

    private String buildTaskContext(List<Task> tasks, Long requestedTaskId, Long requestedSprintId) {
        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort(Comparator
                .comparing((Task task) -> task.getSprint() != null && task.getSprint().getId() != null
                        ? task.getSprint().getId()
                        : Long.MAX_VALUE)
                .thenComparing(task -> task.getStatus() != null ? task.getStatus().name() : "")
                .thenComparing(task -> task.getCreatedAt() != null ? task.getCreatedAt() : LocalDateTime.MIN));

        List<Task> relevantTasks = sortedTasks.stream()
                .filter(task -> requestedTaskId == null || requestedTaskId.equals(task.getId()))
                .filter(task -> requestedSprintId == null || (task.getSprint() != null && requestedSprintId.equals(task.getSprint().getId())))
                .limit(MAX_TASKS_IN_PROMPT)
                .collect(Collectors.toList());

        if (relevantTasks.isEmpty()) {
            relevantTasks = sortedTasks.stream().limit(MAX_TASKS_IN_PROMPT).collect(Collectors.toList());
        }

        if (relevantTasks.isEmpty()) {
            return "- No hay tareas visibles.\n";
        }

        StringBuilder context = new StringBuilder();
        for (Task task : relevantTasks) {
            context.append("- #").append(task.getId())
                    .append(" ").append(valueOrDash(task.getTitle()))
                    .append(" | estado=").append(task.getStatus() != null ? task.getStatus().name() : "-")
                    .append(" | prioridad=").append(task.getPriority() != null ? task.getPriority().name() : "-")
                    .append(" | grupo=").append(getGroupName(task))
                    .append(" | sprint=").append(getSprintLabel(task))
                    .append(" | estimadas=").append(formatHours(task.getEstimatedHours()))
                    .append(" | reales=").append(formatHours(task.getActualHours()))
                    .append(" | asignados=").append(getAssignees(task))
                    .append("\n");
        }
        return context.toString();
    }

    private String buildFallbackAnswer(String question, List<Task> visibleTasks, Long requestedTaskId, Long requestedSprintId) {
        boolean sprintQuestion = isSprintQuestion(question) || requestedSprintId != null;
        if (sprintQuestion) {
            return buildSprintFallbackAnswer(visibleTasks, requestedSprintId);
        }

        return buildTaskFallbackAnswer(question, visibleTasks, requestedTaskId);
    }

    private boolean isSprintQuestion(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        return normalized.contains("sprint")
                || normalized.contains("iteracion")
                || normalized.contains("carga")
                || (normalized.contains("horas de trabajo")
                    && !normalized.contains("tarea")
                    && !normalized.contains("task"));
    }

    private String buildTaskFallbackAnswer(String question, List<Task> visibleTasks, Long requestedTaskId) {
        Optional<Task> requestedTask = findRequestedTask(question, visibleTasks, requestedTaskId);
        double recommendedHours = requestedTask
                .map(task -> calculateRecommendedHours(task, visibleTasks))
                .orElse(calculateRecommendedAverage(visibleTasks));

        if (requestedTask.isPresent()) {
            Task task = requestedTask.get();
            Metrics sprintMetrics = calculateMetrics(getTasksForSameSprint(visibleTasks, task));
            String basis = task.getEstimatedHours() != null
                    ? "La tarea ya tiene horas estimadas registradas."
                    : "La estime con el promedio KPI de tareas similares y el historial visible.";

            return "Estimacion para la tarea #" + task.getId() + " \"" + valueOrDash(task.getTitle()) + "\": "
                    + formatHours(recommendedHours) + " horas.\n"
                    + basis + "\n"
                    + "En su sprint, las horas estimadas restantes son "
                    + formatHours(sprintMetrics.remainingEstimatedHours)
                    + " de " + formatHours(sprintMetrics.totalEstimatedHours) + " horas totales.";
        }

        return "No encontre una tarea exacta en los datos visibles. Con los KPIs actuales, la estimacion sugerida para esa solicitud es "
                + formatHours(recommendedHours)
                + " horas. La base fue el promedio de horas estimadas/reales de tareas visibles.";
    }

    private String buildSprintFallbackAnswer(List<Task> visibleTasks, Long requestedSprintId) {
        List<Task> sprintTasks = resolveSprintTasks(visibleTasks, requestedSprintId);
        Optional<Sprint> sprint = resolveSprint(requestedSprintId, sprintTasks);

        if (sprintTasks.isEmpty()) {
            return "No encontre tareas visibles para ese sprint. Revisa el id del sprint o agrega tareas con horas estimadas para calcular carga de trabajo.";
        }

        Metrics metrics = calculateMetrics(sprintTasks);
        String sprintName = sprint
                .map(value -> value.getName() != null ? value.getName() : "#" + value.getId())
                .orElse(getSprintLabel(sprintTasks.get(0)));
        String dateRange = sprint.map(value -> " (" + formatDateTime(value.getStartDate()) + " a " + formatDateTime(value.getEndDate()) + ")")
                .orElse("");

        return "Sprint " + sprintName + dateRange + ": "
                + formatHours(metrics.totalEstimatedHours) + " horas estimadas en "
                + metrics.taskCount + " tareas.\n"
                + "Restan " + formatHours(metrics.remainingEstimatedHours) + " horas estimadas; ya hay "
                + formatHours(metrics.totalActualHours) + " horas reales registradas.\n"
                + "Estado: " + metrics.pendingCount + " pendientes, "
                + metrics.inProgressCount + " en progreso y "
                + metrics.completedCount + " completadas.";
    }

    private Optional<Task> findRequestedTask(String question, List<Task> tasks, Long requestedTaskId) {
        if (requestedTaskId != null) {
            return tasks.stream()
                    .filter(task -> requestedTaskId.equals(task.getId()))
                    .findFirst();
        }

        String normalizedQuestion = normalize(question);
        Task bestTask = null;
        double bestScore = 0.0;

        for (Task task : tasks) {
            String normalizedTitle = normalize(task.getTitle());
            if (normalizedTitle.isEmpty()) {
                continue;
            }

            if (normalizedQuestion.contains(normalizedTitle)) {
                return Optional.of(task);
            }

            double score = titleMatchScore(normalizedQuestion, normalizedTitle);
            if (score > bestScore) {
                bestScore = score;
                bestTask = task;
            }
        }

        return bestScore >= 0.5 ? Optional.of(bestTask) : Optional.empty();
    }

    private double titleMatchScore(String normalizedQuestion, String normalizedTitle) {
        String[] titleWords = normalizedTitle.split("\\s+");
        int usefulWords = 0;
        int matchedWords = 0;

        for (String word : titleWords) {
            if (word.length() < 4) {
                continue;
            }
            usefulWords++;
            if (normalizedQuestion.contains(word)) {
                matchedWords++;
            }
        }

        if (usefulWords == 0) {
            return 0.0;
        }
        return (double) matchedWords / usefulWords;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String asciiValue = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return asciiValue.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double calculateRecommendedHours(Task task, List<Task> visibleTasks) {
        if (task.getEstimatedHours() != null && task.getEstimatedHours() > 0) {
            return task.getEstimatedHours();
        }

        List<Task> sameGroupTasks = visibleTasks.stream()
                .filter(candidate -> extractGroupId(candidate) != null && extractGroupId(candidate).equals(extractGroupId(task)))
                .collect(Collectors.toList());
        double groupAverage = calculateRecommendedAverage(sameGroupTasks);
        if (groupAverage > 0) {
            return groupAverage;
        }

        return calculateRecommendedAverage(visibleTasks);
    }

    private double calculateRecommendedAverage(List<Task> tasks) {
        List<Double> actualHours = tasks.stream()
                .filter(task -> task.getActualHours() != null && task.getActualHours() > 0)
                .map(task -> task.getActualHours().doubleValue())
                .collect(Collectors.toList());

        if (!actualHours.isEmpty()) {
            return actualHours.stream().mapToDouble(Double::doubleValue).average().orElse(2.0);
        }

        List<Double> estimatedHours = tasks.stream()
                .filter(task -> task.getEstimatedHours() != null && task.getEstimatedHours() > 0)
                .map(task -> task.getEstimatedHours().doubleValue())
                .collect(Collectors.toList());

        if (!estimatedHours.isEmpty()) {
            return estimatedHours.stream().mapToDouble(Double::doubleValue).average().orElse(2.0);
        }

        return 2.0;
    }

    private List<Task> resolveSprintTasks(List<Task> visibleTasks, Long requestedSprintId) {
        if (requestedSprintId != null) {
            return visibleTasks.stream()
                    .filter(task -> task.getSprint() != null && requestedSprintId.equals(task.getSprint().getId()))
                    .collect(Collectors.toList());
        }

        Optional<Sprint> currentSprint = sprintRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (currentSprint.isPresent()) {
            Long currentSprintId = currentSprint.get().getId();
            List<Task> currentSprintTasks = visibleTasks.stream()
                    .filter(task -> task.getSprint() != null && currentSprintId.equals(task.getSprint().getId()))
                    .collect(Collectors.toList());
            if (!currentSprintTasks.isEmpty()) {
                return currentSprintTasks;
            }
        }

        Map<Long, List<Task>> tasksBySprint = visibleTasks.stream()
                .filter(task -> task.getSprint() != null && task.getSprint().getId() != null)
                .collect(Collectors.groupingBy(task -> task.getSprint().getId()));

        return tasksBySprint.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(new ArrayList<>());
    }

    private Optional<Sprint> resolveSprint(Long requestedSprintId, List<Task> sprintTasks) {
        if (requestedSprintId != null) {
            return sprintRepository.findById(requestedSprintId);
        }

        return sprintTasks.stream()
                .map(Task::getSprint)
                .filter(value -> value != null)
                .findFirst();
    }

    private List<Task> getTasksForSameSprint(List<Task> visibleTasks, Task task) {
        if (task.getSprint() == null || task.getSprint().getId() == null) {
            return new ArrayList<>();
        }

        Long sprintId = task.getSprint().getId();
        return visibleTasks.stream()
                .filter(candidate -> candidate.getSprint() != null && sprintId.equals(candidate.getSprint().getId()))
                .collect(Collectors.toList());
    }

    private Map<String, Metrics> groupMetricsBySprint(List<Task> tasks) {
        Map<String, List<Task>> tasksBySprint = tasks.stream()
                .filter(task -> task.getSprint() != null)
                .collect(Collectors.groupingBy(this::getSprintLabel, LinkedHashMap::new, Collectors.toList()));

        Map<String, Metrics> metricsBySprint = new LinkedHashMap<>();
        for (Map.Entry<String, List<Task>> entry : tasksBySprint.entrySet()) {
            metricsBySprint.put(entry.getKey(), calculateMetrics(entry.getValue()));
        }
        return metricsBySprint;
    }

    private Metrics calculateMetrics(List<Task> tasks) {
        Metrics metrics = new Metrics();
        metrics.taskCount = tasks.size();

        for (Task task : tasks) {
            double estimatedHours = task.getEstimatedHours() != null ? task.getEstimatedHours() : 0.0;
            double actualHours = task.getActualHours() != null ? task.getActualHours() : 0.0;
            metrics.totalEstimatedHours += estimatedHours;
            metrics.totalActualHours += actualHours;

            if (task.getStatus() == TaskStatus.completed) {
                metrics.completedCount++;
                metrics.completedEstimatedHours += estimatedHours;
            } else {
                metrics.remainingEstimatedHours += estimatedHours;
                if (task.getStatus() == TaskStatus.in_progress) {
                    metrics.inProgressCount++;
                } else {
                    metrics.pendingCount++;
                }
            }
        }

        return metrics;
    }

    private String getGroupName(Task task) {
        if (task == null || task.getTodoList() == null || task.getTodoList().getGroup() == null) {
            return "-";
        }
        return valueOrDash(task.getTodoList().getGroup().getName());
    }

    private String getSprintLabel(Task task) {
        if (task == null || task.getSprint() == null) {
            return "-";
        }

        String sprintName = task.getSprint().getName() != null ? task.getSprint().getName() : "Sprint";
        return sprintName + " (#" + task.getSprint().getId() + ")";
    }

    private String getAssignees(Task task) {
        List<String> assignees = taskAssignmentRepository.findByTaskId(task.getId()).stream()
                .map(assignment -> assignment.getUser() != null ? assignment.getUser().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());

        if (assignees.isEmpty()) {
            return "-";
        }

        return String.join(", ", assignees);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatHours(Float value) {
        return value == null ? "-" : formatHours(value.doubleValue());
    }

    private String formatHours(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static class Metrics {
        private int taskCount;
        private long pendingCount;
        private long inProgressCount;
        private long completedCount;
        private double totalEstimatedHours;
        private double completedEstimatedHours;
        private double remainingEstimatedHours;
        private double totalActualHours;
    }
}
