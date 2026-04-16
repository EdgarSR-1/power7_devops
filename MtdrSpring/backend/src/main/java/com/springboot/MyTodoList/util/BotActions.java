package com.springboot.MyTodoList.util;

import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.UserType;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            private static final Pattern REGISTER_USER_PATTERN = Pattern.compile(
                "^/?registeruser(?:@\\w+)?\\s+(.+?)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s*$",
            Pattern.CASE_INSENSITIVE
        );
        private static final Pattern REGISTER_USER_HELP_PATTERN = Pattern.compile(
                "^/?registeruser(?:@\\w+)?\\s*$",
            Pattern.CASE_INSENSITIVE
        );
                private static final Pattern START_DEBUG_PATTERN = Pattern.compile(
                        "^/?start(?:@\\w+)?\\s+-d\\s*$",
                    Pattern.CASE_INSENSITIVE
                );
    private static final Map<Long, Long> pendingTaskGroupByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Long> lastViewedGroupByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, String>> taskActionButtonsByChat = new ConcurrentHashMap<>();

    String requestText;
    long chatId;
    Long telegramUserId;
    User requesterUser;
    TelegramClient telegramClient;
    boolean exit;

    ToDoItemService todoService;
    DeepSeekService deepSeekService;
    TaskService taskService;
    TaskGroupService taskGroupService;
    UserService userService;

    public BotActions(TelegramClient tc, ToDoItemService ts, DeepSeekService ds, TaskService tks, TaskGroupService tgs, UserService us){
        telegramClient = tc;
        todoService = ts;
        deepSeekService = ds;
        taskService = tks;
        taskGroupService = tgs;
        userService = us;
        exit  = false;
    }

    public void setRequestText(String cmd){
        requestText=cmd;
    }

    public void setChatId(long chId){
        chatId=chId;
    }

    public void setTelegramUserId(Long tgUserId) {
        telegramUserId = tgUserId;
    }

    public void setRequesterUser(User user) {
        requesterUser = user;
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

    public void setUserService(UserService usvc){
        userService = usvc;
    }

    public UserService getUserService(){
        return userService;
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

    private String statusTag(TaskStatus status) {
        if (status == null) {
            return "[PENDING]";
        }
        switch (status) {
            case completed:
                return "[COMPLETED]";
            case in_progress:
                return "[IN_PROGRESS]";
            case pending:
            default:
                return "[PENDING]";
        }
    }

    private String statusTagFromString(String statusValue) {
        if (statusValue == null) {
            return statusTag(TaskStatus.pending);
        }
        try {
            return statusTag(TaskStatus.valueOf(statusValue));
        } catch (Exception ignored) {
            return "[PENDING]";
        }
    }

    private String buildGroupStatusSummary(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "\n\nNo tasks in this group yet.";
        }

        long pendingCount = tasks.stream().filter(task -> task.getStatus() == TaskStatus.pending).count();
        long inProgressCount = tasks.stream().filter(task -> task.getStatus() == TaskStatus.in_progress).count();
        long completedCount = tasks.stream().filter(task -> task.getStatus() == TaskStatus.completed).count();

        StringBuilder summary = new StringBuilder();
        summary.append("\n\nStatus Summary")
                .append("\nPENDING: ").append(pendingCount)
                .append("\nIN_PROGRESS: ").append(inProgressCount)
                .append("\nCOMPLETED: ").append(completedCount)
                .append("\n\nTasks:");

        for (Task task : tasks) {
            summary.append("\n#")
                    .append(task.getId())
                    .append(" ")
                    .append(statusTag(task.getStatus()))
                    .append(" ")
                    .append(task.getTitle());
        }

        return summary.toString();
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
                taskRow.add(statusTagFromString(task.getStatus()) + " " + task.getTitle());
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
            row.add(statusTag(task.getStatus()) + " " + task.getTitle());
            row.add(registerTaskActionButton("Done #" + task.getId(), TASK_DONE_PREFIX + task.getId()));
            keyboard.add(row);
        }

        for (Task task : doneTasks) {
            KeyboardRow row = new KeyboardRow();
            row.add(statusTag(task.getStatus()) + " " + task.getTitle());
            row.add(registerTaskActionButton("Undo #" + task.getId(), TASK_UNDO_PREFIX + task.getId()));
            row.add(registerTaskActionButton("Delete #" + task.getId(), TASK_DELETE_PREFIX + task.getId()));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, titleMessage + buildGroupStatusSummary(groupTasks), telegramClient, keyboardMarkup);
    }


    

    public void fnStart() {
        boolean isStartCommand = requestText != null && (
                requestText.equals(BotCommands.START_COMMAND.getCommand())
                        || requestText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel())
                        || START_DEBUG_PATTERN.matcher(requestText.trim()).matches()
        );

        if (!isStartCommand || exit)
            return;

        String welcomeMessage = BotMessages.HELLO_MYTODO_BOT.getMessage();
        if (requesterUser != null && requesterUser.getName() != null && !requesterUser.getName().isBlank()) {
            welcomeMessage = "Hello, " + requesterUser.getName().trim() + "!\n" + welcomeMessage;
        }

        String roleMessage = "";
        String userIdText = "N/A";
        String userTypeText = "UNREGISTERED";
        if (requesterUser != null && requesterUser.getUserType() != null) {
            roleMessage = requesterUser.getUserType() == UserType.DEVELOPER
                    ? "\n\n" + BotMessages.ROLE_DEVELOPER.getMessage()
                    : "\n\n" + BotMessages.ROLE_NORMAL.getMessage();
            if (requesterUser.getId() != null) {
                userIdText = String.valueOf(requesterUser.getId());
            }
            userTypeText = requesterUser.getUserType().name();
        }

        String identityDebug = "";
        if (START_DEBUG_PATTERN.matcher(requestText.trim()).matches()) {
            identityDebug = "\n\nDebug Identity\n"
                    + "telegramUserId: " + (telegramUserId != null ? telegramUserId : "N/A") + "\n"
                    + "dbUserId: " + userIdText + "\n"
                    + "userType: " + userTypeText;

            if (requesterUser == null) {
                identityDebug += "\n" + BotMessages.USER_NOT_REGISTERED.getMessage();
            }
        }

        BotHelper.sendMessageToTelegram(chatId, welcomeMessage + roleMessage + identityDebug, telegramClient,  ReplyKeyboardMarkup
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

    public void fnRegisterUser() {
        if (requestText == null || exit) {
            return;
        }

        String normalizedRequest = requestText.trim();

        if (REGISTER_USER_HELP_PATTERN.matcher(normalizedRequest).matches()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_USER_DATA.getMessage(), telegramClient);
            exit = true;
            return;
        }

        String normalizedLower = normalizedRequest.toLowerCase();
        if (!(normalizedLower.startsWith(BotCommands.REGISTER_USER.getCommand())
            || normalizedLower.startsWith(BotCommands.REGISTER_USER.getCommand().substring(1)))) {
            return;
        }

        Matcher matcher = REGISTER_USER_PATTERN.matcher(normalizedRequest);
        if (!matcher.matches()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_USER_DATA.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            User user = new User();
            user.setName(matcher.group(1));
            user.setEmail(matcher.group(2));
            user.setPassword(matcher.group(3));
            user.setPhone(matcher.group(4));
            user.setTelegramUserId(telegramUserId);
            user.setTelegramChatId(chatId);
            if (requesterUser != null && requesterUser.getUserType() != null) {
                user.setUserType(requesterUser.getUserType());
            }

            userService.createUser(user);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.NEW_USER_ADDED.getMessage(), telegramClient);
        } catch (RuntimeException e) {
            logger.error(e.getLocalizedMessage(), e);
            String message = "Email already exists".equals(e.getMessage())
                    ? BotMessages.USER_ALREADY_EXISTS.getMessage()
                    : e.getMessage();
            BotHelper.sendMessageToTelegram(chatId, message, telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_USER_DATA.getMessage(), telegramClient);
        }

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

    public void fnAddTask() {
        if (exit || requestText == null) {
            return;
        }

        String normalizedLower = requestText.toLowerCase().trim();
        if (!normalizedLower.startsWith(BotCommands.ADD_TASK.getCommand())) {
            return;
        }

        String payload = requestText.substring(BotCommands.ADD_TASK.getCommand().length()).trim();
        String[] parts = payload.split("\\s+");
        
        if (parts.length < 2) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ADD_TASK_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        Float estimatedHours = null;
        try {
            estimatedHours = Float.parseFloat(parts[parts.length - 1]);
            if (estimatedHours <= 0 || estimatedHours > 40) {
                BotHelper.sendMessageToTelegram(chatId, "Hours must be between 0.5 and 40.", telegramClient);
                exit = true;
                return;
            }
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_HOURS.getMessage(), telegramClient);
            exit = true;
            return;
        }

        String title = payload.substring(0, payload.lastIndexOf(String.valueOf(estimatedHours))).trim();
        if (title.isEmpty()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ADD_TASK_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            List<TaskGroup> groups = taskGroupService.findAll();
            if (groups.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, "No groups found. Create one first.", telegramClient);
                exit = true;
                return;
            }
            
            TaskGroup defaultGroup = groups.get(0);
            taskService.createTaskInGroupWithHours(defaultGroup.getId(), title, estimatedHours);
            String message = String.format(BotMessages.TASK_ADDED_WITH_HOURS.getMessage(), 
                    estimatedHours, requesterUser != null ? requesterUser.getName() : "Unknown");
            BotHelper.sendMessageToTelegram(chatId, message, telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, "Could not create task: " + e.getMessage(), telegramClient);
        }
        exit = true;
    }

    public void fnStartTask() {
        if (exit || requestText == null) {
            return;
        }

        String normalizedLower = requestText.toLowerCase().trim();
        if (!normalizedLower.startsWith(BotCommands.START_TASK.getCommand())) {
            return;
        }

        String payload = requestText.substring(BotCommands.START_TASK.getCommand().length()).trim();
        Long taskId;

        try {
            taskId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.START_TASK_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            taskService.startTask(taskId);
            String message = String.format(BotMessages.TASK_STARTED.getMessage(), taskId);
            BotHelper.sendMessageToTelegram(chatId, message, telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    public void fnCompleteTask() {
        if (exit || requestText == null) {
            return;
        }

        String normalizedLower = requestText.toLowerCase().trim();
        if (!normalizedLower.startsWith(BotCommands.COMPLETE_TASK.getCommand())) {
            return;
        }

        String payload = requestText.substring(BotCommands.COMPLETE_TASK.getCommand().length()).trim();
        String[] parts = payload.split("\\s+");

        if (parts.length < 2) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.COMPLETE_TASK_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        Long taskId;
        Float actualHours;

        try {
            taskId = Long.parseLong(parts[0]);
            actualHours = Float.parseFloat(parts[1]);
            if (actualHours <= 0 || actualHours > 40) {
                BotHelper.sendMessageToTelegram(chatId, "Hours must be between 0.5 and 40.", telegramClient);
                exit = true;
                return;
            }
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.COMPLETE_TASK_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            taskService.completeTask(taskId, actualHours);
            String message = String.format(BotMessages.TASK_COMPLETED.getMessage(), taskId, actualHours);
            BotHelper.sendMessageToTelegram(chatId, message, telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
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