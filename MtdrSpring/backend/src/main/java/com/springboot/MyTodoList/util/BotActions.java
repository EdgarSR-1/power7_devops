package com.springboot.MyTodoList.util;

import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.SprintService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
    private static final String TASK_START_PREFIX = "TASKSTART::";
    private static final String TASK_MOVE_PREFIX = "TASKMOVE::";
    private static final float MAX_ESTIMATED_HOURS_PER_TASK = 4f;
    private static final int MAX_SPLIT_TASKS = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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
    private static final Map<Long, String> pendingTaskTitleByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Long> lastViewedGroupByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Long> pendingMoveSprintTaskByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Long> pendingCompleteTaskByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, String>> groupSelectionButtonsByChat = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, String>> taskActionButtonsByChat = new ConcurrentHashMap<>();

    String requestText;
    long chatId;
    Long telegramUserId;
    User requesterUser;
    TelegramClient telegramClient;
    boolean exit;

    ToDoItemService todoService;
    DeepSeekService deepSeekService;
    SprintService sprintService;
    TaskService taskService;
    TaskGroupService taskGroupService;
    UserService userService;

    public BotActions(TelegramClient tc, ToDoItemService ts, DeepSeekService ds, SprintService ss, TaskService tks, TaskGroupService tgs, UserService us){
        telegramClient = tc;
        todoService = ts;
        deepSeekService = ds;
        sprintService = ss;
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

    private void clearGroupSelectionButtons() {
        groupSelectionButtonsByChat.put(chatId, new ConcurrentHashMap<>());
    }

    private String registerGroupSelectionButton(String visibleLabel, String actionToken) {
        groupSelectionButtonsByChat.computeIfAbsent(chatId, key -> new ConcurrentHashMap<>()).put(visibleLabel, actionToken);
        return visibleLabel;
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
                || requestText.startsWith(TASK_START_PREFIX)
                || requestText.startsWith(TASK_MOVE_PREFIX)
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

    private String resolveGroupSelectionToken() {
        if (requestText == null) {
            return null;
        }

        if (requestText.startsWith(GROUP_SELECTION_PREFIX) || requestText.startsWith(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel())) {
            return requestText;
        }

        Map<String, String> chatActions = groupSelectionButtonsByChat.get(chatId);
        if (chatActions == null) {
            return null;
        }

        return chatActions.get(requestText);
    }

    private void registerPendingMoveSprintTask(Long taskId) {
        pendingMoveSprintTaskByChat.put(chatId, taskId);
    }

    private Long getPendingMoveSprintTask() {
        return pendingMoveSprintTaskByChat.get(chatId);
    }

    private void clearPendingMoveSprintTask() {
        pendingMoveSprintTaskByChat.remove(chatId);
    }

    private void registerPendingCompleteTask(Long taskId) {
        pendingCompleteTaskByChat.put(chatId, taskId);
    }

    private Long getPendingCompleteTask() {
        return pendingCompleteTaskByChat.get(chatId);
    }

    private void clearPendingCompleteTask() {
        pendingCompleteTaskByChat.remove(chatId);
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
                    .append(task.getTitle())
                    .append("\n  Sprint: ")
                    .append(task.getSprint() != null && task.getSprint().getName() != null ? task.getSprint().getName() : "-")
                    .append("\n  Estimated: ")
                    .append(formatHours(task.getEstimatedHours()))
                    .append("h | Actual: ")
                    .append(formatHours(task.getActualHours()))
                    .append("h")
                    .append("\n  Start: ")
                    .append(formatDateTime(task.getStartDate()))
                    .append(" | End: ")
                    .append(formatDateTime(task.getEndDate()))
                    .append(" | Due: ")
                    .append(formatDateTime(task.getDueDate()));
        }

        return summary.toString();
    }

    private String buildAllTasksSummary(Map<String, List<TaskResponseDTO>> tasksByGroup) {
        if (tasksByGroup == null || tasksByGroup.isEmpty()) {
            return "\n\nNo tasks available.";
        }

        StringBuilder summary = new StringBuilder("\n\nTasks Overview");
        for (Map.Entry<String, List<TaskResponseDTO>> groupEntry : tasksByGroup.entrySet()) {
            summary.append("\n\n[")
                    .append(groupEntry.getKey())
                    .append("]");

            if (groupEntry.getValue().isEmpty()) {
                summary.append("\n- No tasks");
                continue;
            }

            for (TaskResponseDTO task : groupEntry.getValue()) {
                summary.append("\n#")
                        .append(task.getId())
                        .append(" ")
                        .append(statusTagFromString(task.getStatus()))
                        .append(" ")
                        .append(task.getTitle())
                        .append("\n  Sprint: ")
                        .append(task.getSprintName() != null ? task.getSprintName() : "-")
                        .append("\n  Start: ")
                        .append(formatDateTime(task.getStartDate()))
                        .append(" | End: ")
                        .append(formatDateTime(task.getEndDate()))
                        .append(" | Due: ")
                        .append(formatDateTime(task.getDueDate()));
            }
        }

        return summary.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private String formatHours(Float value) {
        return value == null ? "-" : String.format("%.1f", value);
    }

    private Float parseHours(String rawValue) {
        String normalizedValue = rawValue != null ? rawValue.trim().replace(',', '.') : "";
        if (normalizedValue.isEmpty()) {
            throw new NumberFormatException("Hours are required");
        }

        Float hours = Float.parseFloat(normalizedValue);
        if (!Float.isFinite(hours)) {
            throw new NumberFormatException("Hours must be finite");
        }
        return hours;
    }

    private boolean exceedsEstimatedHoursLimit(Float estimatedHours) {
        return estimatedHours > MAX_ESTIMATED_HOURS_PER_TASK * MAX_SPLIT_TASKS;
    }

    private ReplyKeyboardMarkup buildMainMenuKeyboard() {
        return ReplyKeyboardMarkup
                .builder()
                .keyboardRow(new KeyboardRow(BotLabels.LIST_ALL_ITEMS.getLabel(), BotLabels.ADD_NEW_ITEM.getLabel()))
                .keyboardRow(new KeyboardRow(BotLabels.LIST_GROUP_TASKS.getLabel(), BotLabels.CREATE_GROUP.getLabel()))
                .keyboardRow(new KeyboardRow(BotLabels.LIST_SPRINT_TASKS.getLabel(), BotLabels.LIST_SPRINTS.getLabel()))
                .keyboardRow(new KeyboardRow(BotLabels.SHOW_MAIN_SCREEN.getLabel(), BotLabels.HIDE_MAIN_SCREEN.getLabel()))
                .build();
    }

    private void sendMessageWithMainMenu(String message) {
        BotHelper.sendMessageToTelegram(chatId, message, telegramClient, buildMainMenuKeyboard());
    }

    private void sendSprintTasksSummary(Sprint sprint, List<Task> sprintTasks) {
        if (sprintTasks == null || sprintTasks.isEmpty()) {
            sendMessageWithMainMenu(BotMessages.NO_TASKS_IN_SPRINT.getMessage());
            return;
        }

        List<Task> pendingTasks = sprintTasks.stream()
                .filter(task -> task.getStatus() == null || task.getStatus() == TaskStatus.pending)
                .collect(Collectors.toList());
        List<Task> inProgressTasks = sprintTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.in_progress)
                .collect(Collectors.toList());
        List<Task> completedTasks = sprintTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.completed)
                .collect(Collectors.toList());

        StringBuilder summary = new StringBuilder();
        summary.append("Sprint ")
                .append(sprint.getName() != null ? sprint.getName() : "(no name)")
                .append(" (#")
                .append(sprint.getId())
                .append(")\n")
                .append("Range: ")
                .append(formatDateTime(sprint.getStartDate()))
                .append(" -> ")
                .append(formatDateTime(sprint.getEndDate()))
                .append("\n\nStatus Summary")
                .append("\nPENDING: ").append(pendingTasks.size())
                .append("\nIN_PROGRESS: ").append(inProgressTasks.size())
                .append("\nCOMPLETED: ").append(completedTasks.size());

        appendTaskSection(summary, "PENDING", pendingTasks);
        appendTaskSection(summary, "IN_PROGRESS", inProgressTasks);
        appendTaskSection(summary, "COMPLETED", completedTasks);

        sendMessageWithMainMenu(summary.toString());
    }

    private void appendTaskSection(StringBuilder summary, String sectionTitle, List<Task> tasks) {
        summary.append("\n\n[").append(sectionTitle).append("]");
        if (tasks.isEmpty()) {
            summary.append("\n- No tasks");
            return;
        }

        for (Task task : tasks) {
            summary.append("\n#")
                    .append(task.getId())
                    .append(" ")
                    .append(task.getTitle())
                    .append("\n  Estimated: ")
                    .append(formatHours(task.getEstimatedHours()))
                    .append("h | Actual: ")
                    .append(formatHours(task.getActualHours()))
                    .append("h")
                    .append("\n  Start: ")
                    .append(formatDateTime(task.getStartDate()))
                    .append(" | End: ")
                    .append(formatDateTime(task.getEndDate()))
                    .append(" | Due: ")
                    .append(formatDateTime(task.getDueDate()));
        }
    }

    private void renderAllTasksMenu(String titleMessage) {
        lastViewedGroupByChat.remove(chatId);
        clearTaskActionButtons();
        List<TaskResponseDTO> allTasks = taskService.getAllTasks(requesterUser);

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
                } else if (TaskStatus.pending.name().equals(status)) {
                    taskRow.add(registerTaskActionButton("Start #" + task.getId(), TASK_START_PREFIX + task.getId()));
                    taskRow.add(registerTaskActionButton("Move Sprint #" + task.getId(), TASK_MOVE_PREFIX + task.getId()));
                } else {
                    taskRow.add(registerTaskActionButton("Move Sprint #" + task.getId(), TASK_MOVE_PREFIX + task.getId()));
                    taskRow.add(registerTaskActionButton("Done #" + task.getId(), TASK_DONE_PREFIX + task.getId()));
                }
                keyboard.add(taskRow);
            }
        }

        KeyboardRow mainScreenRowBottom = new KeyboardRow();
        mainScreenRowBottom.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(mainScreenRowBottom);

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, titleMessage + buildAllTasksSummary(tasksByGroup), telegramClient, keyboardMarkup);
    }

    private void renderGroupTasksMenu(Long groupId, String titleMessage) {
        lastViewedGroupByChat.put(chatId, groupId);
        clearTaskActionButtons();

        List<Task> groupTasks = taskService.getTasksByGroupId(groupId, requesterUser);
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
            if (task.getStatus() == TaskStatus.pending) {
                row.add(registerTaskActionButton("Start #" + task.getId(), TASK_START_PREFIX + task.getId()));
                row.add(registerTaskActionButton("Move Sprint #" + task.getId(), TASK_MOVE_PREFIX + task.getId()));
            } else {
                row.add(registerTaskActionButton("Move Sprint #" + task.getId(), TASK_MOVE_PREFIX + task.getId()));
                row.add(registerTaskActionButton("Done #" + task.getId(), TASK_DONE_PREFIX + task.getId()));
            }
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

        welcomeMessage += "\n\nSprint commands:\n/sprints";

        String roleMessage = "";
        String userIdText = "N/A";
        String roleText = "UNREGISTERED";

        if (requesterUser != null && requesterUser.getRole() != null) {
            String roleName = requesterUser.getRole().getName().name();

            roleMessage = "\n\nRol: " + roleName;
            roleText = roleName;

            if (requesterUser.getId() != null) {
                userIdText = String.valueOf(requesterUser.getId());
            }
        }

        String identityDebug = "";
        if (START_DEBUG_PATTERN.matcher(requestText.trim()).matches()) {
            identityDebug = "\n\nDebug Identity\n"
                    + "telegramUserId: " + (telegramUserId != null ? telegramUserId : "N/A") + "\n"
                    + "dbUserId: " + userIdText + "\n"
                    + "role: " + roleText;

            if (requesterUser == null) {
                identityDebug += "\n" + BotMessages.USER_NOT_REGISTERED.getMessage();
            }
        }

        BotHelper.sendMessageToTelegram(chatId, welcomeMessage + roleMessage + identityDebug, telegramClient, buildMainMenuKeyboard());
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
        clearGroupSelectionButtons();
        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        KeyboardRow titleRow = new KeyboardRow();
        titleRow.add(BotLabels.SELECT_GROUP.getLabel());
        keyboard.add(titleRow);

        for (TaskGroup group : groups) {
            KeyboardRow row = new KeyboardRow();
            row.add(registerGroupSelectionButton(group.getName(), GROUP_SELECTION_PREFIX + group.getId()));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, "Select a group", telegramClient, keyboardMarkup);
        exit = true;
    }

    public void fnListGroupTasks() {
        if (exit)
            return;

        try {
            String actionToken = resolveGroupSelectionToken();
            if (actionToken == null || !actionToken.startsWith(GROUP_SELECTION_PREFIX)) {
                return;
            }

            String groupIdToken = actionToken.substring(GROUP_SELECTION_PREFIX.length());
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
            registerPendingCompleteTask(taskId);
            sendMessageWithMainMenu("Type the actual hours spent to finish Task #" + taskId + ".");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        exit = true;
    }

    public void fnTaskStart() {
        if (exit)
            return;

        String actionToken = resolveTaskActionToken();
        if (actionToken == null || !actionToken.startsWith(TASK_START_PREFIX))
            return;

        try {
            Long taskId = Long.valueOf(actionToken.substring(TASK_START_PREFIX.length()));
            taskService.startTask(taskId, requesterUser);
            Long groupId = lastViewedGroupByChat.get(chatId);
            if (groupId != null) {
                renderGroupTasksMenu(groupId, "Task started!");
            } else {
                renderAllTasksMenu("Task started!");
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
            taskService.updateTaskStatus(taskId, TaskStatus.pending, requesterUser);
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
            taskService.deleteTask(taskId, requesterUser);
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

    public void fnSprintTasks() {
        if (exit || requestText == null) {
            return;
        }

        String normalizedRequest = requestText.trim();
        boolean requestedFromMenu = normalizedRequest.equals(BotLabels.LIST_SPRINT_TASKS.getLabel());
        boolean requestedFromCommand = normalizedRequest.toLowerCase().startsWith(BotCommands.SPRINT_TASKS.getCommand());

        if (!requestedFromMenu && !requestedFromCommand) {
            return;
        }

        try {
            if (requestedFromMenu || normalizedRequest.equalsIgnoreCase(BotCommands.SPRINT_TASKS.getCommand())) {
                Optional<Sprint> currentSprint = taskService.getCurrentSprint();
                if (currentSprint.isEmpty()) {
                    sendMessageWithMainMenu(BotMessages.NO_CURRENT_SPRINT.getMessage());
                    exit = true;
                    return;
                }

                Sprint sprint = currentSprint.get();
                List<Task> sprintTasks = taskService.getTasksBySprintId(sprint.getId(), requesterUser);
                sendSprintTasksSummary(sprint, sprintTasks);
                exit = true;
                return;
            }

            String payload = normalizedRequest.substring(BotCommands.SPRINT_TASKS.getCommand().length()).trim();
            Long sprintId;
            try {
                sprintId = Long.parseLong(payload);
            } catch (NumberFormatException ex) {
                sendMessageWithMainMenu(BotMessages.SPRINT_TASKS_FORMAT.getMessage());
                exit = true;
                return;
            }

            Sprint sprint = taskService.getSprintById(sprintId);
            List<Task> sprintTasks = taskService.getTasksBySprintId(sprintId, requesterUser);
            sendSprintTasksSummary(sprint, sprintTasks);
        } catch (RuntimeException ex) {
            sendMessageWithMainMenu(ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getLocalizedMessage(), ex);
            sendMessageWithMainMenu(BotMessages.SPRINT_TASKS_FORMAT.getMessage());
        }

        exit = true;
    }

    public void fnListSprints() {
        if (exit || requestText == null) {
            return;
        }

        String normalizedRequest = requestText.trim();
        boolean requestedFromMenu = normalizedRequest.equals(BotLabels.LIST_SPRINTS.getLabel());
        boolean requestedFromCommand = normalizedRequest.toLowerCase().startsWith(BotCommands.SPRINTS.getCommand());

        if (!requestedFromMenu && !requestedFromCommand) {
            return;
        }

        try {
            List<Sprint> sprints = sprintService.findAll();
            if (sprints.isEmpty()) {
                sendMessageWithMainMenu(BotMessages.NO_SPRINTS_FOUND.getMessage());
                exit = true;
                return;
            }

            StringBuilder summary = new StringBuilder("Sprints\n");
            for (Sprint sprint : sprints) {
                summary.append("\n#")
                        .append(sprint.getId())
                        .append(" ")
                        .append(sprint.getName() != null ? sprint.getName() : "(no name)")
                        .append("\n  Start: ")
                        .append(formatDateTime(sprint.getStartDate()))
                        .append(" | End: ")
                        .append(formatDateTime(sprint.getEndDate()))
                        .append("\n");
            }

            sendMessageWithMainMenu(summary.toString());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            sendMessageWithMainMenu(BotMessages.NO_SPRINTS_FOUND.getMessage());
        }

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
            if (requesterUser != null && requesterUser.getRole() != null) {
                 user.setRole(requesterUser.getRole());
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
        clearGroupSelectionButtons();

        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        for (TaskGroup group : groups) {
            KeyboardRow row = new KeyboardRow();
            row.add(registerGroupSelectionButton(group.getName(), BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel() + group.getId()));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, BotMessages.SELECT_GROUP_FOR_NEW_TASK.getMessage(), telegramClient, keyboardMarkup);
        exit = true;
    }

    public void fnSelectGroupForNewTask() {
        if (exit)
            return;

        try {
            String actionToken = resolveGroupSelectionToken();
            if (actionToken == null || !actionToken.startsWith(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel())) {
                return;
            }

            String groupIdToken = actionToken.substring(BotLabels.SELECT_GROUP_FOR_NEW_TASK_PREFIX.getLabel().length());
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

        String input = requestText != null ? requestText.trim() : "";
        if (input.isEmpty() || input.startsWith("/"))
            return;

        try {
            String pendingTitle = pendingTaskTitleByChat.get(chatId);

            if (pendingTitle == null) {
                pendingTaskTitleByChat.put(chatId, input);
                BotHelper.sendMessageToTelegram(
                        chatId,
                        BotMessages.TYPE_NEW_TASK_ESTIMATED_HOURS.getMessage(),
                        telegramClient
                );
                exit = true;
                return;
            }

            Float estimatedHours;
            try {
                estimatedHours = parseHours(input);
            } catch (NumberFormatException e) {
                sendMessageWithMainMenu(BotMessages.INVALID_HOURS.getMessage());
                exit = true;
                return;
            }

            if (estimatedHours <= 0) {
                sendMessageWithMainMenu("Hours must be greater than 0.");
                exit = true;
                return;
            }

            if (exceedsEstimatedHoursLimit(estimatedHours)) {
                sendMessageWithMainMenu(BotMessages.ESTIMATED_HOURS_TOO_LARGE.getMessage());
                exit = true;
                return;
            }

            taskService.createTaskInGroupWithHours(selectedGroupId, pendingTitle, estimatedHours, requesterUser);
            pendingTaskGroupByChat.remove(chatId);
            pendingTaskTitleByChat.remove(chatId);
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
            if (pendingTaskTitleByChat.containsKey(chatId)) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_TASK_ESTIMATED_HOURS.getMessage(), telegramClient, null);
            } else {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_TASK_TITLE.getMessage(), telegramClient, null);
            }
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
            sendMessageWithMainMenu(BotMessages.ADD_TASK_FORMAT.getMessage());
            exit = true;
            return;
        }

        String hoursToken = parts[parts.length - 1];
        Float estimatedHours;
        try {
            estimatedHours = parseHours(hoursToken);
            if (estimatedHours <= 0) {
                sendMessageWithMainMenu("Hours must be greater than 0.");
                exit = true;
                return;
            }
        } catch (NumberFormatException e) {
            sendMessageWithMainMenu(BotMessages.INVALID_HOURS.getMessage());
            exit = true;
            return;
        }

        if (exceedsEstimatedHoursLimit(estimatedHours)) {
            sendMessageWithMainMenu(BotMessages.ESTIMATED_HOURS_TOO_LARGE.getMessage());
            exit = true;
            return;
        }

        String title = payload.substring(0, payload.length() - hoursToken.length()).trim();
        if (title.isEmpty()) {
            sendMessageWithMainMenu(BotMessages.ADD_TASK_FORMAT.getMessage());
            exit = true;
            return;
        }

        try {
            List<TaskGroup> groups = taskGroupService.findAll();
            if (groups.isEmpty()) {
                sendMessageWithMainMenu("No groups found. Create one first.");
                exit = true;
                return;
            }
            
            TaskGroup defaultGroup = groups.get(0);

            if (estimatedHours <= MAX_ESTIMATED_HOURS_PER_TASK) {
                taskService.createTaskInGroupWithHours(defaultGroup.getId(), title, estimatedHours, requesterUser);
                String message = String.format(BotMessages.TASK_ADDED_WITH_HOURS.getMessage(),
                        estimatedHours, requesterUser != null ? requesterUser.getName() : "Unknown");
                sendMessageWithMainMenu(message);
            } else {
                int partsCount = (int) Math.ceil(estimatedHours / MAX_ESTIMATED_HOURS_PER_TASK);
                float remainingHours = estimatedHours;

                for (int i = 1; i <= partsCount; i++) {
                    float splitHours = Math.min(MAX_ESTIMATED_HOURS_PER_TASK, remainingHours);
                    String splitTitle = title + " (Part " + i + "/" + partsCount + ")";
                    taskService.createTaskInGroupWithHours(defaultGroup.getId(), splitTitle, splitHours, requesterUser);
                    remainingHours -= splitHours;
                }

                String message = String.format(
                        BotMessages.TASK_SPLIT_CREATED.getMessage(),
                        estimatedHours,
                        partsCount,
                        requesterUser != null ? requesterUser.getName() : "Unknown"
                );
                sendMessageWithMainMenu(message);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            sendMessageWithMainMenu("Could not create task: " + e.getMessage());
        }
        exit = true;
    }

    public void fnMoveSprint() {
        if (exit || requestText == null) {
            return;
        }

        Long pendingTaskId = getPendingMoveSprintTask();
        String trimmedRequest = requestText.trim();
        if (pendingTaskId != null && trimmedRequest.matches("^\\d+$")) {
            try {
                Long sprintId = Long.valueOf(trimmedRequest);
                taskService.moveTaskToSprint(pendingTaskId, sprintId, requesterUser);
                clearPendingMoveSprintTask();

                Long groupId = lastViewedGroupByChat.get(chatId);
                String message = String.format(BotMessages.TASK_SPRINT_CHANGED.getMessage(), pendingTaskId, sprintId);
                if (groupId != null) {
                    renderGroupTasksMenu(groupId, message);
                } else {
                    renderAllTasksMenu(message);
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                sendMessageWithMainMenu(BotMessages.TASK_SPRINT_NOT_FOUND.getMessage());
            }

            exit = true;
            return;
        }

        String actionToken = resolveTaskActionToken();
        if (actionToken != null && actionToken.startsWith(TASK_MOVE_PREFIX)) {
            try {
                Long taskId = Long.valueOf(actionToken.substring(TASK_MOVE_PREFIX.length()));
                registerPendingMoveSprintTask(taskId);
                sendMessageWithMainMenu(BotMessages.MOVE_SPRINT_PROMPT.getMessage() + " Task #" + taskId + ".");
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                sendMessageWithMainMenu(BotMessages.MOVE_SPRINT_FORMAT.getMessage());
            }

            exit = true;
            return;
        }

        String normalizedLower = requestText.toLowerCase().trim();
        if (!normalizedLower.startsWith(BotCommands.MOVE_SPRINT.getCommand())) {
            return;
        }

        String payload = requestText.substring(BotCommands.MOVE_SPRINT.getCommand().length()).trim();
        String[] parts = payload.split("\\s+");

        if (parts.length < 2) {
            sendMessageWithMainMenu(BotMessages.MOVE_SPRINT_FORMAT.getMessage());
            exit = true;
            return;
        }

        Long taskId;
        Long sprintId;

        try {
            taskId = Long.parseLong(parts[0]);
            sprintId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendMessageWithMainMenu(BotMessages.MOVE_SPRINT_FORMAT.getMessage());
            exit = true;
            return;
        }

        try {
            taskService.moveTaskToSprint(taskId, sprintId, requesterUser);
            String message = String.format(BotMessages.TASK_SPRINT_CHANGED.getMessage(), taskId, sprintId);
            sendMessageWithMainMenu(message);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            sendMessageWithMainMenu(BotMessages.TASK_SPRINT_NOT_FOUND.getMessage());
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
            sendMessageWithMainMenu(BotMessages.START_TASK_FORMAT.getMessage());
            exit = true;
            return;
        }

        try {
            taskService.startTask(taskId, requesterUser);
            String message = String.format(BotMessages.TASK_STARTED.getMessage(), taskId);
            sendMessageWithMainMenu(message);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            sendMessageWithMainMenu(BotMessages.TASK_NOT_FOUND.getMessage());
        }
        exit = true;
    }

    public void fnCompleteTask() {
        if (exit || requestText == null) {
            return;
        }

        Long pendingTaskId = getPendingCompleteTask();
        String trimmedRequest = requestText.trim();
        if (pendingTaskId != null && trimmedRequest.matches("^\\d+(?:[.,]\\d+)?$")) {
            try {
                Float actualHours = Float.parseFloat(trimmedRequest.replace(',', '.'));
                if (actualHours <= 0 || actualHours > 40) {
                    sendMessageWithMainMenu("Hours must be between 0.5 and 40.");
                    exit = true;
                    return;
                }

                taskService.completeTask(pendingTaskId, actualHours, requesterUser);
                clearPendingCompleteTask();

                Long groupId = lastViewedGroupByChat.get(chatId);
                String message = String.format(BotMessages.TASK_COMPLETED.getMessage(), pendingTaskId, actualHours);
                if (groupId != null) {
                    renderGroupTasksMenu(groupId, message);
                } else {
                    renderAllTasksMenu(message);
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                sendMessageWithMainMenu(BotMessages.TASK_NOT_FOUND.getMessage());
            }

            exit = true;
            return;
        }

        if (pendingTaskId != null) {
            sendMessageWithMainMenu("Type the actual hours spent to finish Task #" + pendingTaskId + ".");
            exit = true;
            return;
        }

        String normalizedLower = requestText.toLowerCase().trim();
        if (!normalizedLower.startsWith(BotCommands.COMPLETE_TASK.getCommand())) {
            return;
        }

        String payload = requestText.substring(BotCommands.COMPLETE_TASK.getCommand().length()).trim();
        String[] parts = payload.split("\\s+");

        if (parts.length < 2) {
            sendMessageWithMainMenu(BotMessages.COMPLETE_TASK_FORMAT.getMessage());
            exit = true;
            return;
        }

        Long taskId;
        Float actualHours;

        try {
            taskId = Long.parseLong(parts[0]);
            actualHours = Float.parseFloat(parts[1]);
            if (actualHours <= 0 || actualHours > 40) {
                sendMessageWithMainMenu("Hours must be between 0.5 and 40.");
                exit = true;
                return;
            }
        } catch (NumberFormatException e) {
            sendMessageWithMainMenu(BotMessages.COMPLETE_TASK_FORMAT.getMessage());
            exit = true;
            return;
        }

        try {
            taskService.completeTask(taskId, actualHours, requesterUser);
            String message = String.format(BotMessages.TASK_COMPLETED.getMessage(), taskId, actualHours);
            sendMessageWithMainMenu(message);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            sendMessageWithMainMenu(BotMessages.TASK_NOT_FOUND.getMessage());
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
