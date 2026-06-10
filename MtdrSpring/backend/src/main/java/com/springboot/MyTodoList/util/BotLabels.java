package com.springboot.MyTodoList.util;

public enum BotLabels {
	
	// cambie los botones a español, chequen a ver si aun funciona, sino para cambiar otras secciones
	SHOW_MAIN_SCREEN("Mostrar pantalla principal"), 
	HIDE_MAIN_SCREEN("Ocultar pantalla principal"),
	LIST_ALL_ITEMS("Listar todos los elementos"), 
	LIST_GROUP_TASKS("Listar tareas del grupo"),
	LIST_SPRINT_TASKS("Tareas de sprint"),
	LIST_SPRINTS("Listar sprints"),
	CREATE_SPRINT("Crear Sprint"),
	CREATE_GROUP("Create Group"),
	ADD_NEW_ITEM("Agregar Nuevo Elemento"),
	DONE("HECHO"),
	UNDO("DESHACER"),
	DELETE("ELIMINAR"),
	SELECT_GROUP("Seleccionar Grupo"),
	SELECT_GROUP_FOR_NEW_TASK_PREFIX("ADDTASKGROUP::"),
	NEW_GROUP_PREFIX("NEWGROUP-"),
	MY_TODO_LIST("MY TODO LIST"),
	SWITCH_USER("Cambiar Usuario"),
	DASH("-");

	private String label;

	BotLabels(String enumLabel) {
		this.label = enumLabel;
	}

	public String getLabel() {
		return label;
	}

}
