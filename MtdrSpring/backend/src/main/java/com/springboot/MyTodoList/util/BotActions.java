package com.springboot.MyTodoList.util;

import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ToDoItemService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class BotActions{

    private static final Logger logger = LoggerFactory.getLogger(BotActions.class);
    private static final String GROUP_SELECTION_PREFIX = "GROUP::";
    private static final String TASK_DONE_PREFIX = "TASKDONE::";
    private static final String TASK_UNDO_PREFIX = "TASKUNDO::";
    private static final String TASK_DELETE_PREFIX = "TASKDEL::";
    private static final Map<Long, Long> pendingTaskGroupByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Long> lastViewedGroupByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, String>> taskActionButtonsByChat = new ConcurrentHashMap<>();

    String requestText;
    long chatId;
    TelegramClient telegramClient;
    boolean exit;

    ToDoItemService todoService;
    DeepSeekService deepSeekService;
    TaskService taskService;
    TaskGroupService taskGroupService;

    public BotActions(TelegramClient tc, ToDoItemService ts, DeepSeekService ds, TaskService tks, TaskGroupService tgs){
        telegramClient = tc;
        todoService = ts;
        deepSeekService = ds;
        taskService = tks;
        taskGroupService = tgs;
        exit  = false;
    }

    public void setRequestText(String cmd){
        requestText=cmd;
    }

    public void setChatId(long chId){
        chatId=chId;
    }

    public void setTelegramClient(TelegramClient tc){
        telegramClient=tc;
    }

    public void setTodoService(ToDoItemService tsvc){
        todoService = tsvc;
    }

    public ToDoItemService getTodoService(){
        return todoService;
    }

    public void setDeepSeekService(DeepSeekService dssvc){
        deepSeekService = dssvc;
    }

    public DeepSeekService getDeepSeekService(){
        return deepSeekService;
    }

    private void clearTaskActionButtons() {
        taskActionButtonsByChat.put(chatId, new ConcurrentHashMap<>());
    }

    private String registerTaskActionButton(String visibleLabel, String actionToken) {
        taskActionButtonsByChat.computeIfAbsent(chatId, key -> new ConcurrentHashMap<>()).put(visibleLabel, actionToken);
        return visibleLabel;
    }

    private String resolveTaskActionToken() {
        if (requestText == null) {
            return null;
        }
        if (requestText.startsWith(TASK_DONE_PREFIX)
                || requestText.startsWith(TASK_UNDO_PREFIX)
                || requestText.startsWith(TASK_DELETE_PREFIX)) {
            return requestText;
        }
        Map<String, String> chatActions = taskActionButtonsByChat.get(chatId);
        if (chatActions == null) {
            return null;
        }
        return chatActions.get(requestText);
    }

    private void renderAllTasksMenu(String titleMessage) {
        lastViewedGroupByChat.remove(chatId);
        clearTaskActionButtons();
        List<TaskResponseDTO> allTasks = taskService.getAllTasks();

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow mainScreenRowTop = new KeyboardRow();
        mainScreenRowTop.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(mainScreenRowTop);

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BotLabels.ADD_NEW_ITEM.getLabel());
        keyboard.add(firstRow);

        Map<String, List<TaskResponseDTO>> tasksByGroup = new LinkedHashMap<>();
        for (TaskResponseDTO task : allTasks) {
            String groupName = task.getGroupName() != null ? task.getGroupName() : "No Group";
            tasksByGroup.computeIfAbsent(groupName, key -> new ArrayList<>()).add(task);
        }

        for (Map.Entry<String, List<TaskResponseDTO>> groupEntry : tasksByGroup.entrySet()) {
            KeyboardRow groupTitleRow = new KeyboardRow();
            groupTitleRow.add("[" + groupEntry.getKey() + "]");
            keyboard.add(groupTitleRow);

            for (TaskResponseDTO task : groupEntry.getValue()) {
                KeyboardRow taskRow = new KeyboardRow();
                taskRow.add(task.getTitle());
                String status = task.getStatus() != null ? task.getStatus() : TaskStatus.pending.name();
                if (TaskStatus.completed.name().equals(status)) {
                    taskRow.add(registerTaskActionButton("Undo #" + task.getId(), TASK_UNDO_PREFIX + task.getId()));
                    taskRow.add(registerTaskActionButton("Delete #" + task.getId(), TASK_DELETE_PREFIX + task.getId()));
                } else {
                    taskRow.add(registerTaskActionButton("Done #" + task.getId(), TASK_DONE_PREFIX + task.getId()));
                }
                keyboard.add(taskRow);
            }
        }

        KeyboardRow mainScreenRowBottom = new KeyboardRow();
        mainScreenRowBottom.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(mainScreenRowBottom);

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, titleMessage, telegramClient, keyboardMarkup);
    }

    private void renderGroupTasksMenu(Long groupId, String titleMessage) {
        lastViewedGroupByChat.put(chatId, groupId);
        clearTaskActionButtons();

        List<Task> groupTasks = taskService.getTasksByGroupId(groupId);
        List<Task> activeTasks = groupTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.completed)
                .collect(Collectors.toList());
        List<Task> doneTasks = groupTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.completed)
                .collect(Collectors.toList());

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        KeyboardRow actionsRow = new KeyboardRow();
        actionsRow.add(BotLabels.SELECT_GROUP.getLabel());
        keyboard.add(actionsRow);

        for (Task task : activeTasks) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(registerTaskActionButton("Done #" + task.getId(), TASK_DONE_PREFIX + task.getId()));
            keyboard.add(row);
        }

        for (Task task : doneTasks) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(registerTaskActionButton("Undo #" + task.getId(), TASK_UNDO_PREFIX + task.getId()));
            row.add(registerTaskActionButton("Delete #" + task.getId(), TASK_DELETE_PREFIX + task.getId()));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, titleMessage, telegramClient, keyboardMarkup);
    }


    

    public void fnStart() {
        if (!(requestText.equals(BotCommands.START_COMMAND.getCommand()) || requestText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel())) || exit) 
            return;

        BotHelper.sendMessageToTelegram(chatId, BotMessages.HELLO_MYTODO_BOT.getMessage(), telegramClient,  ReplyKeyboardMarkup
            .builder()
            .keyboardRow(new KeyboardRow(BotLabels.LIST_ALL_ITEMS.getLabel(),BotLabels.ADD_NEW_ITEM.getLabel()))
            .keyboardRow(new KeyboardRow(BotLabels.LIST_GROUP_TASKS.getLabel(), BotLabels.CREATE_GROUP.getLabel()))
            .keyboardRow(new KeyboardRow(BotLabels.SHOW_MAIN_SCREEN.getLabel(),BotLabels.HIDE_MAIN_SCREEN.getLabel()))
            .build()
        );
        exit = true;
    }

    public void fnCreateGroupPrompt() {
        if (!(requestText.equals(BotLabels.CREATE_GROUP.getLabel())) || exit)
            return;

        BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_GROUP_NAME.getMessage(), telegramClient);
        exit = true;
    }

    public void fnCreateGroup() {
        if (!requestText.startsWith(BotLabels.NEW_GROUP_PREFIX.getLabel()) || exit)
            return;

        try {
            String groupName = requestText.substring(BotLabels.NEW_GROUP_PREFIX.getLabel().length()).trim();
            if (groupName.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_GROUP_NAME.getMessage(), telegramClient);
                exit = true;
                return;
            }

            taskGroupService.createGroupForBot(groupName);

            BotHelper.sendMessageToTelegram(chatId, BotMessages.NEW_GROUP_ADDED.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, "Could not create group", telegramClient);
        }

        exit = true;
    }

    public void fnListGroups() {
        if (!(requestText.equals(BotLabels.LIST_GROUP_TASKS.getLabel())
                || requestText.equals(BotLabels.SELECT_GROUP.getLabel())) || exit)
            return;

        List<TaskGroup> groups = taskGroupService.findAll();
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
            .resizeKeyboard(true)
            .oneTimeKeyboard(false)
            .selective(true)
            .build();

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        KeyboardRow titleRow = new KeyboardRow();
        titleRow.add(BotLabels.SELECT_GROUP.getLabel());
        keyboard.add(titleRow);

        for (TaskGroup group : groups) {
            KeyboardRow row = new KeyboardRow();
            row.add(GROUP_SELECTION_PREFIX + group.getId() + BotLabels.DASH.getLabel() + group.getName());
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, "Select a group", telegramClient, keyboardMarkup);
        exit = true;
    }

    public void fnListGroupTasks() {
        if (!requestText.startsWith(GROUP_SELECTION_PREFIX) || exit)
            return;

        try {
            String payload = requestText.substring(GROUP_SELECTION_PREFIX.length());
            String groupIdToken = payload.contains(BotLabels.DASH.getLabel())
                    ? payload.substring(0, payload.indexOf(BotLabels.DASH.getLabel()))
                    : payload;
            Long groupId = Long.valueOf(groupIdToken);

                renderGroupTasksMenu(groupId, "Group tasks");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, "Could not load tasks for this group", telegramClient);
        }

        exit = true;
    }

    public void fnTaskDone() {
        if (exit)
            return;

        String actionToken = resolveTaskActionToken();
        if (actionToken == null || !actionToken.startsWith(TASK_DONE_PREFIX))
            return;

        try {
            Long taskId = Long.valueOf(actionToken.substring(TASK_DONE_PREFIX.length()));
            taskService.updateTaskStatus(taskId, TaskStatus.completed);
            Long groupId = lastViewedGroupByChat.get(chatId);
            if (groupId != null) {
                renderGroupTasksMenu(groupId, BotMessages.ITEM_DONE.getMessage());
            } else {
                renderAllTasksMenu(BotMessages.ITEM_DONE.getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnTaskUndo() {
        if (exit)
            return;

        String actionToken = resolveTaskActionToken();
        if (actionToken == null || !actionToken.startsWith(TASK_UNDO_PREFIX))
            return;

        try {
            Long taskId = Long.valueOf(actionToken.substring(TASK_UNDO_PREFIX.length()));
            taskService.updateTaskStatus(taskId, TaskStatus.pending);
            Long groupId = lastViewedGroupByChat.get(chatId);
            if (groupId != null) {
                renderGroupTasksMenu(groupId, BotMessages.ITEM_UNDONE.getMessage());
            } else {
                renderAllTasksMenu(BotMessages.ITEM_UNDONE.getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnTaskDelete() {
        if (exit)
            return;

        String actionToken = resolveTaskActionToken();
        if (actionToken == null || !actionToken.startsWith(TASK_DELETE_PREFIX))
            return;

        try {
            Long taskId = Long.valueOf(actionToken.substring(TASK_DELETE_PREFIX.length()));
            taskService.deleteTask(taskId);
            Long groupId = lastViewedGroupByChat.get(chatId);
            if (groupId != null) {
                renderGroupTasksMenu(groupId, BotMessages.ITEM_DELETED.getMessage());
            } else {
                renderAllTasksMenu(BotMessages.ITEM_DELETED.getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnDone() {
        if (exit || requestText == null || !requestText.matches("^\\d+-DONE$"))
            return;
            
        String done = requestText.substring(0, requestText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(done);

        try {

            ToDoItem item = todoService.getToDoItemById(id);
            item.setDone(true);
            todoService.updateToDoItem(id, item);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DONE.getMessage(), telegramClient);

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnUndo() {
        if (exit || requestText == null || !requestText.matches("^\\d+-UNDO$"))
            return;

        String undo = requestText.substring(0,
                requestText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(undo);

        try {

            ToDoItem item = todoService.getToDoItemById(id);
            item.setDone(false);
            todoService.updateToDoItem(id, item);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_UNDONE.getMessage(), telegramClient);

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnDelete(){
        if (exit || requestText == null || !requestText.matches("^\\d+-DELETE$"))
            return;

        String delete = requestText.substring(0,
                requestText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(delete);

        try {
            todoService.deleteToDoItem(id);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DELETED.getMessage(), telegramClient);

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnHide(){
        if (requestText.equals(BotCommands.HIDE_COMMAND.getCommand())
				|| requestText.equals(BotLabels.HIDE_MAIN_SCREEN.getLabel()) && !exit)
			BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), telegramClient);
        else
            return;
        exit = true;
    }

    public void fnListAll(){
        if (!(requestText.equals(BotCommands.TODO_LIST.getCommand())
				|| requestText.equals(BotLabels.LIST_ALL_ITEMS.getLabel())
				|| requestText.equals(BotLabels.MY_TODO_LIST.getLabel())) || exit)
            return;
        renderAllTasksMenu("Tasks grouped by group");
        exit = true;
    }

    public void fnAddItem(){
        logger.info("Adding item");
		if (!(requestText.contains(BotCommands.ADD_ITEM.getCommand())
				|| requestText.contains(BotLabels.ADD_NEW_ITEM.getLabel())) || exit )
            return;

        List<TaskGroup> groups = taskGroupService.findAll();
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        for (TaskGroup group : groups) {
            KeyboardRow row = new KeyboardRow();
            row.add(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel() + group.getId() + BotLabels.DASH.getLabel() + group.getName());
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, BotMessages.SELECT_GROUP_FOR_NEW_TASK.getMessage(), telegramClient, keyboardMarkup);
        exit = true;
    }

    public void fnSelectGroupForNewTask() {
        if (!requestText.startsWith(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel()) || exit)
            return;

        try {
            String payload = requestText.substring(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel().length());
            String groupIdToken = payload.contains(BotLabels.DASH.getLabel())
                    ? payload.substring(0, payload.indexOf(BotLabels.DASH.getLabel()))
                    : payload;
            Long groupId = Long.valueOf(groupIdToken);
            pendingTaskGroupByChat.put(chatId, groupId);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_TASK_TITLE.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.SELECT_GROUP_FOR_NEW_TASK.getMessage(), telegramClient);
        }

        exit = true;
    }

    public void fnCreateTaskFromSelectedGroup() {
        if (exit)
            return;

        Long selectedGroupId = pendingTaskGroupByChat.get(chatId);
        if (selectedGroupId == null)
            return;

        String title = requestText != null ? requestText.trim() : "";
        if (title.isEmpty() || title.startsWith("/"))
            return;

        try {
            taskService.createTaskInGroup(selectedGroupId, title);
            pendingTaskGroupByChat.remove(chatId);
            renderGroupTasksMenu(selectedGroupId, BotMessages.NEW_ITEM_ADDED.getMessage());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, "Could not create task in selected group", telegramClient);
        }

        exit = true;
    }

    public void fnElse(){
        if(exit)
            return;
        if (pendingTaskGroupByChat.containsKey(chatId)) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_TASK_TITLE.getMessage(), telegramClient, null);
            exit = true;
        }
    }

    public void fnLLM(){
        logger.info("Calling LLM");
        if (!(requestText.contains(BotCommands.LLM_REQ.getCommand())) || exit)
            return;
        
        String prompt = "Dame los datos del clima en mty";
        String out = "<empty>";
        try{
            out = deepSeekService.generateText(prompt);
        }catch(Exception exc){

        }

        BotHelper.sendMessageToTelegram(chatId, "LLM: "+out, telegramClient, null);

    }


}