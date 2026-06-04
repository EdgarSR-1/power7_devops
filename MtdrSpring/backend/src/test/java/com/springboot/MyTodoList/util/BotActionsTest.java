package com.springboot.MyTodoList.util;

import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotActionsTest {

    private TelegramClient telegramClient;
    private ToDoItemService toDoItemService;
    private DeepSeekService deepSeekService;
    private SprintService sprintService;
    private TaskService taskService;
    private TaskGroupService taskGroupService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        telegramClient = Mockito.mock(TelegramClient.class);
        toDoItemService = Mockito.mock(ToDoItemService.class);
        deepSeekService = Mockito.mock(DeepSeekService.class);
        sprintService = Mockito.mock(SprintService.class);
        taskService = Mockito.mock(TaskService.class);
        taskGroupService = Mockito.mock(TaskGroupService.class);
        userService = Mockito.mock(UserService.class);
    }

    private BotActions newActions() {
        return new BotActions(
                telegramClient,
                toDoItemService,
                deepSeekService,
                sprintService,
                taskService,
                taskGroupService,
                userService
        );
    }

    @Test
    void fnStartShouldSendWelcomeMessageAndMainMenu() {
        BotActions actions = newActions();
        User requesterUser = new User();
        requesterUser.setName("Ana");

        actions.setChatId(123L);
        actions.setRequestText("/start");
        actions.setRequesterUser(requesterUser);

        actions.fnStart();

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        Assertions.assertThat(sentMessage.getText()).contains("Hello, Ana!");
        Assertions.assertThat(sentMessage.getText()).contains("Sprint commands:");
        Assertions.assertThat(sentMessage.getReplyMarkup()).isInstanceOf(ReplyKeyboardMarkup.class);

        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();
        Assertions.assertThat(keyboard.getKeyboard()).hasSize(5);
        Assertions.assertThat(keyboard.getKeyboard().get(0))
                .containsExactly(BotLabels.LIST_ALL_ITEMS.getLabel(), BotLabels.ADD_NEW_ITEM.getLabel());
    }

    @Test
    void fnRegisterUserShouldCreateUserAndConfirm() {
        BotActions actions = newActions();

        actions.setChatId(456L);
        actions.setTelegramUserId(999L);
        actions.setRequestText("/registeruser Carlos carlos@example.com secret123 5512345678");

        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        actions.fnRegisterUser();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        Assertions.assertThat(createdUser.getName()).isEqualTo("Carlos");
        Assertions.assertThat(createdUser.getEmail()).isEqualTo("carlos@example.com");
        Assertions.assertThat(createdUser.getPassword()).isEqualTo("secret123");
        Assertions.assertThat(createdUser.getPhone()).isEqualTo("5512345678");
        Assertions.assertThat(createdUser.getTelegramUserId()).isEqualTo(999L);
        Assertions.assertThat(createdUser.getTelegramChatId()).isEqualTo(456L);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .isEqualTo(BotMessages.NEW_USER_ADDED.getMessage());
    }

    @Test
    void fnRegisterUserWithoutDataShouldAskForUserFormat() {
        BotActions actions = newActions();

        actions.setChatId(789L);
        actions.setRequestText("/registeruser");

        actions.fnRegisterUser();

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .isEqualTo(BotMessages.TYPE_NEW_USER_DATA.getMessage());
    }

    @Test
    void fnCreateGroupShouldCreateGroupAndConfirm() {
        BotActions actions = newActions();

        actions.setChatId(1001L);
        actions.setRequestText("NEWGROUP-Backend Team");

        actions.fnCreateGroup();

        verify(taskGroupService).createGroupForBot("Backend Team");

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .isEqualTo(BotMessages.NEW_GROUP_ADDED.getMessage());
    }

    @Test
    void fnAddTaskShouldCreateTaskInDefaultGroup() {
        BotActions actions = newActions();
        User requesterUser = new User();
        requesterUser.setName("Ana");

        TaskGroup group = new TaskGroup();
        group.setId(7L);
        group.setName("Backend Team");

        when(taskGroupService.findAll()).thenReturn(List.of(group));

        actions.setChatId(1002L);
        actions.setRequesterUser(requesterUser);
        actions.setRequestText("/addtask Implement login 3");

        actions.fnAddTask();

        verify(taskService).createTaskInGroupWithHours(7L, "Implement login", 3f, requesterUser);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .contains("Task created with estimated hours: 3.0!");
    }

    @Test
    void fnStartTaskShouldMoveTaskToInProgress() {
        BotActions actions = newActions();
        User requesterUser = new User();
        requesterUser.setName("Ana");

        when(taskService.getAllTasks(any())).thenReturn(List.of());

        actions.setChatId(1003L);
        actions.setRequesterUser(requesterUser);
        actions.setRequestText("TASKSTART::10");

        actions.fnTaskStart();

        verify(taskService).startTask(10L, requesterUser);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .contains("Task started!");
    }

    @Test
    void fnDoneThenCompleteTaskShouldFinishTask() {
        User requesterUser = new User();
        requesterUser.setName("Ana");

        BotActions askForHours = newActions();
        askForHours.setChatId(1004L);
        askForHours.setRequesterUser(requesterUser);
        askForHours.setRequestText("TASKDONE::11");

        askForHours.fnTaskDone();

        ArgumentCaptor<SendMessage> firstMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(firstMessageCaptor.capture());
        Assertions.assertThat(firstMessageCaptor.getValue().getText())
                .contains("Type the actual hours spent to finish Task #11.");

        BotActions completeTask = newActions();
        when(taskService.getAllTasks(any())).thenReturn(List.of());

        completeTask.setChatId(1004L);
        completeTask.setRequesterUser(requesterUser);
        completeTask.setRequestText("2.5");

        completeTask.fnCompleteTask();

        verify(taskService).completeTask(11L, 2.5f, requesterUser);

        ArgumentCaptor<SendMessage> secondMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, Mockito.times(2)).execute(secondMessageCaptor.capture());
        Assertions.assertThat(secondMessageCaptor.getAllValues().get(1).getText())
                .contains("Task 11 marked as COMPLETED!");
    }

    @Test
    void fnTaskDeleteShouldDeleteTask() {
        BotActions actions = newActions();
        User requesterUser = new User();
        requesterUser.setName("Ana");

        when(taskService.getAllTasks(any())).thenReturn(List.of());

        actions.setChatId(1005L);
        actions.setRequesterUser(requesterUser);
        actions.setRequestText("TASKDEL::12");

        actions.fnTaskDelete();

        verify(taskService).deleteTask(12L, requesterUser);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());
        Assertions.assertThat(messageCaptor.getValue().getText())
                .contains("Item deleted!");
    }
}