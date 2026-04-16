package com.springboot.MyTodoList.util;

public enum BotCommands {

	START_COMMAND("/start"), 
	HIDE_COMMAND("/hide"), 
	TODO_LIST("/todolist"),
	REGISTER_USER("/registeruser"),
	ADD_ITEM("/additem"),
	ADD_TASK("/addtask"),
	SPRINTS("/sprints"),
	CREATE_SPRINT("/createsprint"),
	MOVE_SPRINT("/movesprint"),
	SPRINT_TASKS("/sprinttasks"),
	START_TASK("/starttask"),
	COMPLETE_TASK("/completetask"),
	LLM_REQ("/llm");

	private String command;

	BotCommands(String enumCommand) {
		this.command = enumCommand;
	}

	public String getCommand() {
		return command;
	}
}
