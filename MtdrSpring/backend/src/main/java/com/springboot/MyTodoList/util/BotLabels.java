package com.springboot.MyTodoList.util;

public enum BotLabels {
	
	SHOW_MAIN_SCREEN("Show Main Screen"), 
	HIDE_MAIN_SCREEN("Hide Main Screen"),
	LIST_ALL_ITEMS("List All Items"), 
	LIST_GROUP_TASKS("List Tasks By Group"),
	CREATE_GROUP("Create Group"),
	ADD_NEW_ITEM("Add New Item"),
	DONE("DONE"),
	UNDO("UNDO"),
	DELETE("DELETE"),
	SELECT_GROUP("Select Group"),
	SELECT_GROUP_FOR_NEW_TASK_PREFIX("ADDTASKGROUP::"),
	NEW_GROUP_PREFIX("NEWGROUP-"),
	MY_TODO_LIST("MY TODO LIST"),
	DASH("-");

	private String label;

	BotLabels(String enumLabel) {
		this.label = enumLabel;
	}

	public String getLabel() {
		return label;
	}

}
