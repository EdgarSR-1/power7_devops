package com.springboot.MyTodoList.util;

public enum BotMessages {
	
	HELLO_MYTODO_BOT(
		"Hola! Soy PowerSeven Bot!\n Escribe una nueva tarea abajo y presiona el botón de enviar (flecha azul), o selecciona una opción de las siguientes:"),
	BOT_REGISTERED_STARTED("Bot registrado y iniciado correctamente!"),
	TYPE_LOGIN_DATA("Escribe tus datos asi: /login email@ejemplo.com password"),
	LOGIN_SUCCESS("Sesion iniciada correctamente! Escribe /start para ir a la pantalla principal."),
	LOGIN_INVALID("Correo o contraseña incorrectos."),
	LOGIN_ALREADY_LINKED("Ese usuario ya esta vinculado a otra cuenta de Telegram. Primero desvinculalo o pide a un admin que limpie TELEGRAM_USER_ID."),
	TYPE_NEW_USER_DATA("Para crear una cuenta nueva usa: /registeruser Nombre email@ejemplo.com password telefono"),
	NEW_USER_ADDED("Usuario creado exitosamente y vinculado a Telegram! Escribe /start para ir a la pantalla principal."),
	USER_ALREADY_EXISTS("Un usuario con ese correo electrónico ya existe."),
	INVALID_USER_DATA("Formato inválido. Usa /registeruser Nombre email@ejemplo.com password telefono"),
	USER_NOT_REGISTERED("Acceso denegado. Tu cuenta de Telegram no esta vinculada. Usa /login email@ejemplo.com password. Si aun no tienes cuenta, usa /registeruser Nombre email@ejemplo.com password telefono."),
	ROLE_DEVELOPER("Autenticado como DEVELOPER. Tienes permisos elevados."),
	ROLE_NORMAL("Autenticado como usuario NORMAL."),
	ITEM_DONE("Tarea completada! Escribe /todolist para regresar a la lista de tareas, o /start para ir a la pantalla principal."), 
	ITEM_UNDONE("Tarea no completada! Selecciona /todolist para regresar a la lista de tareas, o /start para ir a la pantalla principal."), 
	ITEM_DELETED("Tarea eliminada! Selecciona /todolist para regresar a la lista de tareas, o /start para ir a la pantalla principal."),
	TYPE_NEW_TODO_ITEM("Escribe una nueva tarea abajo y presiona el botón de enviar (flecha azul) en el lado derecho."),
	SELECT_GROUP_FOR_NEW_TASK("Selecciona el grupo donde quieres agregar la nueva tarea."),
	TYPE_NEW_TASK_TITLE("Ahora escribe el título de la tarea."),
	TYPE_NEW_TASK_ESTIMATED_HOURS("Ahora escribe las horas estimadas para esta tarea."),
	TYPE_NEW_GROUP_NAME("Escribe el nombre del grupo como este: NEWGROUP-My Team"),
	NEW_ITEM_ADDED("¡Nuevo elemento agregado! Selecciona /todolist para regresar a la lista de tareas, o /start para ir a la pantalla principal."),
	NEW_GROUP_ADDED("¡Grupo creado exitosamente! Usa List Group para ver sus tareas."),
	ADD_TASK_FORMAT("Formato: /addtask titulo_tarea horas_estimadas\nEjemplo: /addtask Implementar login 3\nMáximo por tarea es 4h. Si envías más, el bot lo dividirá automáticamente."),
	TASK_ADDED_WITH_HOURS("Tarea creada con horas estimadas: %s! Desarrollador: %s"),
	TASK_SPLIT_CREATED("La estimación de la tarea fue de %s horas. Se crearon %s subtareas (máximo 4h cada una). Desarrollador: %s"),
	SPRINTS_FORMAT("Usa /sprints para listar todos los sprints."),
	NO_SPRINTS_FOUND("No se encontraron sprints disponibles."),
	SPRINT_CREATED("Sprint creado exitosamente: %s (#%s)"),
	CREATE_SPRINT_FORMAT("Formato: /createsprint <name>;<start>;<end>[;<groupId>]\nEjemplo: /createsprint Sprint 5;2026-04-15 09:00;2026-04-29 18:00;1\nFormatos de fecha soportados: yyyy-MM-dd HH:mm, yyyy-MM-dd'T'HH:mm, dd/MM/yyyy HH:mm, yyyy-MM-dd\nConsejo: también puedes usar | en lugar de ;\nSi se omite groupId, el bot usa el primer grupo existente."),
	MOVE_SPRINT_FORMAT("Formato: /movesprint [task_id] [sprint_id]\nEjemplo: /movesprint 42 3"),
	MOVE_SPRINT_PROMPT("Escribe el ID del sprint de destino y presiona enviar."),
	TASK_SPRINT_CHANGED("Tarea %s movida al sprint %s."),
	TASK_SPRINT_NOT_FOUND("Tarea o sprint no encontrado."),
	SPRINT_TASKS_FORMAT("Usa /sprinttasks para el sprint actual o /sprinttasks [sprint_id] para un sprint específico. Ejemplo: /sprinttasks 2"),
	NO_CURRENT_SPRINT("No se encontró un sprint activo para la fecha/hora actual."),
	NO_TASKS_IN_SPRINT("No se encontraron tareas para este sprint."),
	START_TASK_FORMAT("Formato: /starttask [task_id]\nEjemplo: /starttask 42"),
	TASK_STARTED("Tarea %s marcada como IN_PROGRESS!"),
	COMPLETE_TASK_FORMAT("Formato: /completetask [task_id] [actual_hours]\nEjemplo: /completetask 42 2.5"),
	TASK_COMPLETED("Tarea %s marcada como COMPLETED! Tiempo registrado: %s horas"),
	TASK_NOT_FOUND("Tarea no encontrada. Verifica el ID de la tarea."),
	INVALID_HOURS("Formato de horas inválido. Usa decimal (e.g., 2.5)"),
	BYE("¡Hasta luego! Selecciona /start para reanudar.");

	private String message;

	BotMessages(String enumMessage) {
		this.message = enumMessage;
	}

	public String getMessage() {
		return message;
	}

}
