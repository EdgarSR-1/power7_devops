package com.springboot.MyTodoList.agent;

import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.User;
import org.springframework.stereotype.Component;

@Component
public class AgentOrchestrator {

    private final LlmIntentParser llmIntentParser;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final TaskGroupService taskGroupService;

    public AgentOrchestrator(
            LlmIntentParser llmIntentParser,
            SprintRepository sprintRepository,
            TaskRepository taskRepository,
            TaskService taskService,
            TaskGroupService taskGroupService) {
        this.llmIntentParser = llmIntentParser;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.taskGroupService = taskGroupService;
    }

    public String handleMessage(String messageText) {
        return handleMessage(messageText, null);
    }

    public String handleMessage(String messageText, User requesterUser) {
        ParsedIntent parsedIntent = llmIntentParser.parse(messageText);

        if (parsedIntent.isClarificationNeeded()) {
            return parsedIntent.getClarificationQuestion();
        }

        IntentType intent = parsedIntent.getIntent();
        if (intent == IntentType.HELP) {
            return helpText();
        } else if (intent == IntentType.LIST_TASKS) {
            return formatTasks("Estas son las tareas registradas:", taskRepository.findAll());
        } else if (intent == IntentType.LIST_TASKS_BY_ASSIGNEE) {
            String assignee = parsedIntent.getAssignee();
            if (assignee == null || assignee.isBlank()) {
                return "No especificaste a qué integrante te refieres.";
            }
            List<Task> byAssignee = taskRepository.findAll().stream()
                .filter(t -> t.getCreatedBy() != null && assignee.equalsIgnoreCase(t.getCreatedBy().getName()))
                .collect(Collectors.toList());
            return formatTasks("Estas son las tareas de " + safe(assignee) + ":", byAssignee);
        } else if (intent == IntentType.LIST_TASKS_BY_STATUS) {
            String statusStr = parsedIntent.getStatus();
            if (statusStr == null || statusStr.isBlank()) {
                return "No especificaste el estado (PENDING, IN_PROGRESS, DONE).";
            }
            TaskStatus mapped = mapStatus(statusStr);
            if (mapped == null) {
                return "Estado no reconocido. Usa PENDING, IN_PROGRESS o DONE.";
            }
            List<Task> byStatus = taskRepository.findAll().stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equals(mapped))
                .collect(Collectors.toList());
            return formatTasks("Estas son las tareas con estado " + safe(statusStr) + ":", byStatus);
        } else if (intent == IntentType.LIST_TASKS_BY_SPRINT) {
            return tasksBySprint(parsedIntent.getSprintName());
        } else if (intent == IntentType.CREATE_TASK) {
            return createTask(parsedIntent, requesterUser);
        } else if (intent == IntentType.START_TASK) {
            return startTask(parsedIntent, requesterUser);
        } else if (intent == IntentType.COMPLETE_TASK) {
            return completeTask(parsedIntent, requesterUser);
        } else if (intent == IntentType.REOPEN_TASK) {
            return reopenTask(parsedIntent, requesterUser);
        } else if (intent == IntentType.DELETE_TASK) {
            return deleteTask(parsedIntent, requesterUser);
        } else if (intent == IntentType.CURRENT_SPRINT_SUMMARY) {
            return sprintSummary();
        } else if (intent == IntentType.TEAM_LOAD_SUMMARY) {
            return teamLoadSummary();
        } else if (intent == IntentType.GUACAMOLE_RECIPE) {
            return guacamoleRecipe();
        } else {
            return "No pude interpretar la solicitud. Escribe ayuda para ver ejemplos.";
        }
    }

    private String createTask(ParsedIntent parsedIntent, User requesterUser) {
        if (parsedIntent.getTitle() == null || parsedIntent.getTitle().isBlank()) {
            return "Necesito el titulo de la tarea para poder crearla.";
        }

        if (parsedIntent.getStoryPoints() == null) {
            return "Para crear la tarea necesito las horas estimadas. Ejemplo: crea una tarea para revisar API en grupo Backend con 3 puntos.";
        }

        if (parsedIntent.getStoryPoints() <= 0 || parsedIntent.getStoryPoints() > 40) {
            return "Las horas estimadas deben estar entre 1 y 40.";
        }

        if (requesterUser == null) {
            return "No pude identificar tu usuario para crear la tarea. Usa el bot de Telegram con una cuenta vinculada.";
        }

        List<TaskGroup> groups = taskGroupService.findAccessibleGroups(requesterUser);
        if (groups == null || groups.isEmpty()) {
            return "No tienes grupos accesibles para crear tareas. Primero crea o selecciona un grupo en el menu del bot.";
        }

        String requestedGroupName = parsedIntent.getGroupName();
        if (requestedGroupName == null || requestedGroupName.isBlank()) {
            StringJoiner groupNames = new StringJoiner(", ");
            for (TaskGroup group : groups) {
                if (group.getName() != null && !group.getName().isBlank()) {
                    groupNames.add(group.getName());
                }
            }

            String availableGroups = groupNames.length() > 0 ? groupNames.toString() : "(sin nombres de grupo)";
            return "Para crear la tarea, dime en qué grupo quieres crearla.\n" +
                "Grupos disponibles: " + availableGroups + "\n" +
                "Ejemplo: crea una tarea para preparar release en grupo Backend con 3 puntos";
        }

        TaskGroup selectedGroup = groups.stream()
                .filter(g -> g.getName() != null && g.getName().equalsIgnoreCase(requestedGroupName.trim()))
                .findFirst()
                .orElse(null);

        if (selectedGroup == null) {
            return "No encontré el grupo '" + requestedGroupName + "' entre tus grupos accesibles.";
        }

        try {
            Float estimatedHours = parsedIntent.getStoryPoints().floatValue();
            TaskResponseDTO created = taskService.createTaskInGroupWithHours(
                    selectedGroup.getId(),
                    parsedIntent.getTitle().trim(),
                    estimatedHours,
                    requesterUser
            );

            String sprintLabel = created.getSprintName() != null ? created.getSprintName() : "Sin sprint";
            return "Tarea creada correctamente.\n" +
                "ID: #" + created.getId() + "\n" +
                "Titulo: " + created.getTitle() + "\n" +
                "Estado: " + created.getStatus() + "\n" +
                "Grupo: " + selectedGroup.getName() + "\n" +
                "Sprint: " + sprintLabel;
        } catch (Exception ex) {
            return "No pude crear la tarea: " + ex.getMessage();
        }
    }

    private String startTask(ParsedIntent parsedIntent, User requesterUser) {
        if (requesterUser == null) {
            return "No pude identificar tu usuario para iniciar la tarea.";
        }
        if (parsedIntent.getTaskId() == null) {
            return "Necesito el ID de la tarea. Ejemplo: inicia la tarea 23.";
        }
        try {
            TaskResponseDTO updated = taskService.startTask(parsedIntent.getTaskId(), requesterUser);
            return "Tarea #" + updated.getId() + " marcada como IN_PROGRESS.";
        } catch (Exception ex) {
            return "No pude iniciar la tarea #" + parsedIntent.getTaskId() + ": " + ex.getMessage();
        }
    }

    private String completeTask(ParsedIntent parsedIntent, User requesterUser) {
        if (requesterUser == null) {
            return "No pude identificar tu usuario para completar la tarea.";
        }
        if (parsedIntent.getTaskId() == null) {
            return "Necesito el ID de la tarea. Ejemplo: completa la tarea 23.";
        }
        if (parsedIntent.getStoryPoints() == null) {
            return "Para completar la tarea necesito horas reales. Ejemplo: completa la tarea 23 con 2 puntos.";
        }
        if (parsedIntent.getStoryPoints() <= 0 || parsedIntent.getStoryPoints() > 40) {
            return "Las horas reales deben estar entre 1 y 40.";
        }
        try {
            Float actualHours = parsedIntent.getStoryPoints().floatValue();
            TaskResponseDTO updated = taskService.completeTask(parsedIntent.getTaskId(), actualHours, requesterUser);
            return "Tarea #" + updated.getId() + " marcada como COMPLETED.";
        } catch (Exception ex) {
            return "No pude completar la tarea #" + parsedIntent.getTaskId() + ": " + ex.getMessage();
        }
    }

    private String reopenTask(ParsedIntent parsedIntent, User requesterUser) {
        if (requesterUser == null) {
            return "No pude identificar tu usuario para reabrir la tarea.";
        }
        if (parsedIntent.getTaskId() == null) {
            return "Necesito el ID de la tarea. Ejemplo: reabre la tarea 23.";
        }
        try {
            TaskResponseDTO updated = taskService.updateTaskStatus(parsedIntent.getTaskId(), TaskStatus.pending, requesterUser);
            return "Tarea #" + updated.getId() + " devuelta a PENDING.";
        } catch (Exception ex) {
            return "No pude reabrir la tarea #" + parsedIntent.getTaskId() + ": " + ex.getMessage();
        }
    }

    private String deleteTask(ParsedIntent parsedIntent, User requesterUser) {
        if (requesterUser == null) {
            return "No pude identificar tu usuario para eliminar la tarea.";
        }
        if (parsedIntent.getTaskId() == null) {
            return "Necesito el ID de la tarea. Ejemplo: elimina la tarea 23.";
        }
        try {
            taskService.deleteTask(parsedIntent.getTaskId(), requesterUser);
            return "Tarea #" + parsedIntent.getTaskId() + " eliminada correctamente.";
        } catch (Exception ex) {
            return "No pude eliminar la tarea #" + parsedIntent.getTaskId() + ": " + ex.getMessage();
        }
    }

    private String tasksBySprint(String sprintName) {
        Optional<Sprint> sprintOptional;
        if (sprintName == null || sprintName.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            sprintOptional = sprintRepository
                    .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(now, now);
        } else {
            String normalizedSprint = sprintName.trim();
            sprintOptional = sprintRepository.findAll().stream()
                    .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(normalizedSprint))
                    .findFirst();
        }

        if (sprintOptional.isEmpty()) {
            return "No encontre ese sprint.";
        }

        Sprint sprint = sprintOptional.get();
        List<Task> sprintTasks = taskRepository.findAll().stream()
                .filter(t -> t.getSprint() != null && sprint.getId().equals(t.getSprint().getId()))
                .collect(Collectors.toList());

        return formatTasks("Tareas del sprint " + sprint.getName() + ":", sprintTasks);
    }

    private String sprintSummary() {
        LocalDateTime now = LocalDateTime.now();
        Sprint sprint = sprintRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(now, now)
                .orElse(null);

        if (sprint == null) {
            return "No hay sprints activos en este momento.";
        }

        List<Task> sprintTasks = taskRepository.findAll().stream()
                .filter(t -> t.getSprint() != null && t.getSprint().getId() != null && t.getSprint().getId().equals(sprint.getId()))
                .collect(Collectors.toList());

        long done = sprintTasks.stream().filter(task -> TaskStatus.completed.equals(task.getStatus())).count();
        long inProgress = sprintTasks.stream().filter(task -> TaskStatus.in_progress.equals(task.getStatus())).count();
        long pending = sprintTasks.stream().filter(task -> TaskStatus.pending.equals(task.getStatus())).count();

        return "Resumen del sprint actual\n" +
            "Sprint: " + sprint.getName() + "\n" +
            "Inicio: " + sprint.getStartDate() + "\n" +
            "Fin: " + sprint.getEndDate() + "\n" +
            "Tareas: " + sprintTasks.size() + "\n" +
            "COMPLETED: " + done + "\n" +
            "IN_PROGRESS: " + inProgress + "\n" +
            "PENDING: " + pending;
    }

    private String teamLoadSummary() {
        Map<String, Integer> totals = new HashMap<>();
        List<Task> tasks = taskRepository.findAll();

        StringJoiner joiner = new StringJoiner("\n", "Carga actual del equipo\n", "");
        if (tasks.isEmpty()) {
            joiner.add("No hay tareas asignadas.");
        } else {
            // build totals by assignee (uses createdBy as assignee when present)
            for (Task t : tasks) {
                String who = (t.getCreatedBy() != null && t.getCreatedBy().getName() != null) ? t.getCreatedBy().getName() : "Sin asignar";
                totals.put(who, totals.getOrDefault(who, 0) + 1);
            }

            totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> joiner.add("- " + entry.getKey() + ": " + entry.getValue() + " tareas"));
        }
        return joiner.toString().trim();
    }

    private TaskStatus mapStatus(String statusStr) {
        if (statusStr == null) return null;
        String s = statusStr.trim().toUpperCase();
        switch (s) {
            case "PENDING":
                return TaskStatus.pending;
            case "IN_PROGRESS":
            case "INPROGRESS":
                return TaskStatus.in_progress;
            case "DONE":
            case "COMPLETED":
                return TaskStatus.completed;
            default:
                return null;
        }
    }

    private String guacamoleRecipe() {
        return "Claro. Guacamole rapido:\n" +
            "1. Machaca 2 aguacates maduros.\n" +
            "2. Agrega tomate, cebolla y cilantro picados.\n" +
            "3. Mezcla jugo de limon, sal y chile al gusto.\n" +
            "4. Sirvelo fresco con totopos.";
    }

    private String formatTasks(String title, List<Task> tasks) {
        if (tasks.isEmpty()) {
            return title + "\nNo encontre tareas para ese criterio.";
        }

        StringJoiner joiner = new StringJoiner("\n", title + "\n", "");
        for (Task task : tasks) {
            joiner.add("- " + task.getId() + " [" + task.getStatus().name() + "] " + task.getTitle() + " | " +
                (task.getDescription() != null ? task.getDescription() : "Sin descripcion"));
        }
        return joiner.toString().trim();
    }

    private String helpText() {
        return "🤖 *Asistente de Gestión Ágil*\n" +
            "\n" +
            "Puedo ejecutar consultas y acciones sobre sprints y tareas. Usa cualquiera de estas frases exactas o su variante natural:\n" +
            "\n" +
            "📋 *Consultar Tareas (ejemplos exactos):*\n" +
            "- lista de tareas\n" +
            "- todas las tareas\n" +
            "- qué tareas tiene Ana\n" +
            "- tareas pendientes\n" +
            "- tareas en progreso\n" +
            "- tareas completadas\n" +
            "- tareas del sprint actual\n" +
            "- tareas del sprint Beta\n" +
            "\n" +
            "📊 *Información del Sprint (ejemplos exactos):*\n" +
            "- cómo va el sprint\n" +
            "- resumen del sprint\n" +
            "- estado del sprint actual\n" +
            "\n" +
            "👥 *Carga de Trabajo (ejemplos exactos):*\n" +
            "- quién tiene más carga\n" +
            "- carga del equipo\n" +
            "- distribución de trabajo\n" +
            "\n" +
            "✏️ *Crear Tareas (ejemplos exactos):*\n" +
            "- crea una tarea para [descripción] en grupo [nombre_grupo] y asignala a [nombre] con [N] puntos\n" +
            "- crea una tarea para revisar el código en grupo Backend y asignala a Luis con 5 puntos\n" +
            "\n" +
            "✅ *Cambiar estado de tareas (ejemplos exactos):*\n" +
            "- inicia la tarea 23\n" +
            "- completa la tarea 23 con 2 puntos\n" +
            "- reabre la tarea 23\n" +
            "- elimina la tarea 23\n" +
            "\n" +
            "🔁 *Atajos ejecutables (slash commands si están configurados):*\n" +
            "- /sprints  -> lista sprints disponibles\n" +
            "- /tasks   -> lista tareas (equivalente a 'lista de tareas')\n" +
            "- /help    -> muestra esta ayuda\n" +
            "\n" +
            "💡 *Consejos:*\n" +
            "- Puedes usar lenguaje natural o alguno de los ejemplos exactos\n" +
            "- Si pido aclaración, responde con la información solicitada (por ejemplo, nombre o título)\n" +
            "- Si quieres que ejecute una acción (crear/actualizar), asegúrate de incluir el título y destinatario";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "sin filtro" : value;
    }
}
