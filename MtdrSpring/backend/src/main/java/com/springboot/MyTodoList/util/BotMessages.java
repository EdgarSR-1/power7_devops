package com.springboot.MyTodoList.util;

public enum BotMessages {
	
	HELLO_MYTODO_BOT(
	"Hello! I'm PowerSeven Bot!\nType a new todo item below and press the send button (blue arrow), or select an option below:"),
	BOT_REGISTERED_STARTED("Bot registered and started succesfully!"),
	TYPE_NEW_USER_DATA("Type the user data like this: /registeruser Name email@example.com password phone"),
	NEW_USER_ADDED("User created successfully! Select /start to go to the main screen."),
	USER_ALREADY_EXISTS("A user with that email already exists."),
	INVALID_USER_DATA("Invalid format. Use /registeruser Name email@example.com password phone"),
	USER_NOT_REGISTERED("Access denied. Your Telegram account is not registered. Use /registeruser Name email@example.com password phone or share your contact."),
	ROLE_DEVELOPER("Authenticated as DEVELOPER. You have elevated permissions."),
	ROLE_NORMAL("Authenticated as NORMAL user."),
	ITEM_DONE("Item done! Select /todolist to return to the list of todo items, or /start to go to the main screen."), 
	ITEM_UNDONE("Item undone! Select /todolist to return to the list of todo items, or /start to go to the main screen."), 
	ITEM_DELETED("Item deleted! Select /todolist to return to the list of todo items, or /start to go to the main screen."),
	TYPE_NEW_TODO_ITEM("Type a new todo item below and press the send button (blue arrow) on the rigth-hand side."),
	SELECT_GROUP_FOR_NEW_TASK("Select the group where you want to add the new task."),
	TYPE_NEW_TASK_TITLE("Now type the task title."),
	TYPE_NEW_GROUP_NAME("Type the group name like this: NEWGROUP-My Team"),
	NEW_ITEM_ADDED("New item added! Select /todolist to return to the list of todo items, or /start to go to the main screen."),
	NEW_GROUP_ADDED("Group created successfully! Use List Group to view its tasks."),
	ADD_TASK_FORMAT("Format: /addtask Título de tarea, horas estimadas\nExample: /addtask Implementar login 3"),
	TASK_ADDED_WITH_HOURS("Task created with estimated hours: %s! Developer: %s"),
	START_TASK_FORMAT("Format: /starttask [task_id]\nExample: /starttask 42"),
	TASK_STARTED("Task %s marked as IN_PROGRESS!"),
	COMPLETE_TASK_FORMAT("Format: /completetask [task_id] [actual_hours]\nExample: /completetask 42 2.5"),
	TASK_COMPLETED("Task %s marked as COMPLETED! Time logged: %s hours"),
	TASK_NOT_FOUND("Task not found. Check task ID."),
	INVALID_HOURS("Invalid hours format. Use decimal (e.g., 2.5)"),
	BYE("Bye! Select /start to resume!");

	private String message;

	BotMessages(String enumMessage) {
		this.message = enumMessage;
	}

	public String getMessage() {
		return message;
	}

}
