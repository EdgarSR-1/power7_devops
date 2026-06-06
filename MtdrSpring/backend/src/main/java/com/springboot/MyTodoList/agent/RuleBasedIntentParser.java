package com.springboot.MyTodoList.agent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIntentParser implements IntentParser {

    private static final Pattern CREATE_TASK_PATTERN =
        Pattern.compile("crea(?:r)? una tarea para (.+?)(?: y asigna(?:la)? a ([a-zA-ZáéíóúÁÉÍÓÚñÑ ]+))?(?: con (\\d+) puntos?)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public ParsedIntent parse(String messageText) {
        String text = messageText == null ? "" : messageText.trim();
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[¿?¡!]", "");
        ParsedIntent intent = new ParsedIntent();

        // Only allowed off-topic question.
        if (normalized.matches(".*(c[oó]mo|preparar|hacer|receta|sirve|servir).*(guacamole).*") ||
            normalized.matches(".*(guacamole).*(c[oó]mo|preparar|hacer|receta|sirve|servir).*")) {
            intent.setIntent(IntentType.GUACAMOLE_RECIPE);
            return intent;
        }

        // HELP intent
        if (normalized.matches(".*(ayuda|help|start|que puedo hacer|que puedes hacer|comandos|ejemplos).*")) {
            intent.setIntent(IntentType.HELP);
            return intent;
        }

        // CURRENT_SPRINT_SUMMARY intent
        if (normalized.matches(".*(sprint actual|como va.*sprint|estado del sprint|resumen del sprint|progreso del sprint|que tal el sprint).*")) {
            intent.setIntent(IntentType.CURRENT_SPRINT_SUMMARY);
            return intent;
        }

        // TEAM_LOAD_SUMMARY intent
        if (normalized.matches(".*(quien tiene.*carga|carga del equipo|carga de trabajo|quien esta mas cargado|distribucion de carga|workload).*")) {
            intent.setIntent(IntentType.TEAM_LOAD_SUMMARY);
            return intent;
        }

        // LIST_TASKS_BY_ASSIGNEE intent - multiple patterns
        if ((normalized.matches(".*(tareas|trabajos|actividades).*(tiene|asignadas?|cargo|responsable).+") ||
            normalized.matches(".*(que).*(tareas|trabajos|actividades|hace).*(tiene|está).+") ||
            normalized.matches(".*(tareas|trabajos|actividades).*(de).+")) &&
            !normalized.contains("?") && !normalized.contains("que pasa") && !normalized.contains("que es")) {
            
            intent.setIntent(IntentType.LIST_TASKS_BY_ASSIGNEE);
            
            // Extract assignee name
            String assignee = null;
            if (normalized.contains("tareas tiene ")) {
                assignee = normalized.substring(normalized.indexOf("tareas tiene ") + "tareas tiene ".length()).trim();
            } else if (normalized.contains("tareas asignadas a ")) {
                assignee = normalized.substring(normalized.indexOf("tareas asignadas a ") + "tareas asignadas a ".length()).trim();
            } else if (normalized.contains("tareas de ")) {
                assignee = normalized.substring(normalized.indexOf("tareas de ") + "tareas de ".length()).trim();
            } else if (normalized.contains("trabajos de ")) {
                assignee = normalized.substring(normalized.indexOf("trabajos de ") + "trabajos de ".length()).trim();
            } else if (normalized.contains("actividades de ")) {
                assignee = normalized.substring(normalized.indexOf("actividades de ") + "actividades de ".length()).trim();
            } else if (normalized.matches(".*(tareas|trabajos).*(tiene|asignadas?).+")) {
                assignee = normalized.replaceAll(".*(tareas|trabajos)\\s+(tiene|asignadas?)\\s+", "").trim();
            }
            
            if (assignee != null && !assignee.isBlank() && assignee.length() > 1 && !assignee.equals("de")) {
                intent.setAssignee(capitalize(cleanName(assignee)));
                return intent;
            }
        }

        // LIST_TASKS_BY_STATUS intent
        if (normalized.matches(".*(tareas|actividades).*(pendiente|progreso|en proceso|in progress|done|terminada|completada|hecha).*") ||
            normalized.matches(".*(que|muestra?).*(tareas|actividades|trabajos).*(siguen|están|estan).*") ||
            normalized.matches(".*(tareas|actividades).*(siguen|están|estan).*(pendiente|en progreso|hecha).*")) {
            
            intent.setIntent(IntentType.LIST_TASKS_BY_STATUS);
            
            if (normalized.contains("done") || normalized.contains("completada") || normalized.contains("terminada") || normalized.contains("hecha")) {
                intent.setStatus("DONE");
            } else if (normalized.contains("progreso") || normalized.contains("en proceso") || normalized.contains("in progress") || normalized.contains("haciendo")) {
                intent.setStatus("IN_PROGRESS");
            } else if (normalized.contains("pendiente") || normalized.contains("por hacer") || normalized.contains("sin hacer")) {
                intent.setStatus("PENDING");
            } else {
                intent.setStatus("PENDING");
            }
            return intent;
        }

        // LIST_TASKS intent
        if (normalized.matches(".*(lista|todas?).*(tareas|actividades|trabajos|todos).*") ||
            normalized.matches(".*(muestra?|dame|quiero ver).*(tareas|todo|lista).*") ||
            normalized.matches(".*(tareas|actividades).*(registrada|existe).*") ||
            normalized.equals("/todolist")) {
            
            intent.setIntent(IntentType.LIST_TASKS);
            return intent;
        }

        // CREATE_TASK intent - improved pattern matching
        Matcher matcher = CREATE_TASK_PATTERN.matcher(text);
        if (matcher.find()) {
            intent.setIntent(IntentType.CREATE_TASK);
            intent.setTitle(matcher.group(1) == null ? null : matcher.group(1).trim());
            intent.setAssignee(matcher.group(2) == null ? null : capitalize(matcher.group(2).trim()));
            intent.setStoryPoints(matcher.group(3) == null ? null : Integer.parseInt(matcher.group(3)));
            return intent;
        }

        intent.setIntent(IntentType.UNKNOWN);
        intent.setClarificationNeeded(true);
        intent.setClarificationQuestion("No entendi bien la solicitud. Prueba con: ayuda, lista de tareas, tareas de [persona], resumen del sprint, o carga del equipo.");
        return intent;
    }
    
    private String cleanName(String name) {
        return name.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").trim();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
