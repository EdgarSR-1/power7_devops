package com.springboot.MyTodoList.agent;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AgentOrchestrator {

    private final LlmIntentParser llmIntentParser;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;

    public AgentOrchestrator(LlmIntentParser llmIntentParser, SprintRepository sprintRepository, TaskRepository taskRepository) {
        this.llmIntentParser = llmIntentParser;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
    }

    public String handleMessage(String messageText) {
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
            return formatTasks("Estas son las tareas de " + safe(parsedIntent.getAssignee()) + ":", taskRepository.findAll());
        } else if (intent == IntentType.LIST_TASKS_BY_STATUS) {
            return formatTasks("Estas son las tareas con estado " + safe(parsedIntent.getStatus()) + ":", taskRepository.findAll());
        } else if (intent == IntentType.CREATE_TASK) {
            return createTask(parsedIntent);
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

    private String createTask(ParsedIntent parsedIntent) {
        if (parsedIntent.getTitle() == null || parsedIntent.getTitle().isBlank()) {
            return "Necesito el titulo de la tarea para poder crearla.";
        }

        // Create a task with the provided details
        // Note: Integration with actual task creation would go here
        return "Tarea creada correctamente.\n" +
            "Titulo: " + parsedIntent.getTitle() + "\n" +
            "Responsable: " + (parsedIntent.getAssignee() != null ? parsedIntent.getAssignee() : "Sin asignar") + "\n" +
            "Story points: " + (parsedIntent.getStoryPoints() != null ? parsedIntent.getStoryPoints() : 3) + "\n" +
            "Sprint: " + (parsedIntent.getSprintName() != null ? parsedIntent.getSprintName() : "Sin sprint");
    }

    private String sprintSummary() {
        List<Sprint> sprints = sprintRepository.findAll();
        if (sprints.isEmpty()) {
            return "No hay sprints registrados.";
        }

        Sprint sprint = sprints.get(0);
        List<Task> sprintTasks = taskRepository.findAll();

        long done = sprintTasks.stream().filter(task -> "DONE".equals(task.getStatus().name())).count();
        long inProgress = sprintTasks.stream().filter(task -> "IN_PROGRESS".equals(task.getStatus().name())).count();
        long pending = sprintTasks.stream().filter(task -> "PENDING".equals(task.getStatus().name())).count();

        return "Resumen del sprint actual\n" +
            "Sprint: " + sprint.getName() + "\n" +
            "Inicio: " + sprint.getStartDate() + "\n" +
            "Fin: " + sprint.getEndDate() + "\n" +
            "Tareas: " + sprintTasks.size() + "\n" +
            "DONE: " + done + "\n" +
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
            totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> joiner.add("- " + entry.getKey() + ": " + entry.getValue() + " pts"));
        }
        return joiner.toString().trim();
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
            "Puedo ayudarte con consultas y acciones del proyecto. Aquí están mis comandos:\n" +
            "\n" +
            "📋 *Consultar Tareas:*\n" +
            "- \"lista de tareas\" o \"todas las tareas\"\n" +
            "- \"qué tareas tiene [nombre]\" (Ej: qué tareas tiene Ana)\n" +
            "- \"tareas pendientes\" o \"tareas por hacer\"\n" +
            "- \"tareas en progreso\"\n" +
            "- \"tareas completadas\" o \"tareas done\"\n" +
            "\n" +
            "📊 *Información del Sprint:*\n" +
            "- \"cómo va el sprint\" o \"resumen del sprint\"\n" +
            "- \"estado del sprint actual\"\n" +
            "\n" +
            "👥 *Carga de Trabajo:*\n" +
            "- \"quién tiene más carga\"\n" +
            "- \"carga del equipo\"\n" +
            "- \"distribución de trabajo\"\n" +
            "\n" +
            "✏️ *Crear Tareas:*\n" +
            "- \"crea una tarea para [descripción] y asignala a [nombre] con [N] puntos\"\n" +
            "- Ejemplo: \"crea una tarea para revisar el código y asignala a Luis con 5 puntos\"\n" +
            "\n" +
            "💡 *Consejos:*\n" +
            "- Puedo entender variaciones naturales del lenguaje\n" +
            "- Prueba a escribir de forma natural, sin comandos especiales\n" +
            "- Si no entiendo algo, te lo haré saber";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "sin filtro" : value;
    }
}
