package com.springboot.MyTodoList.util;

import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
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
    private User requesterUser;

    @BeforeEach
    void setUp() {
        telegramClient = mock(TelegramClient.class);
        toDoItemService = mock(ToDoItemService.class);
        deepSeekService = mock(DeepSeekService.class);
        sprintService = mock(SprintService.class);
        taskService = mock(TaskService.class);
        taskGroupService = mock(TaskGroupService.class);
        userService = mock(UserService.class);

        requesterUser = new User();
        requesterUser.setName("Developer Test");
    }

    @Test
    void addTaskCommandAcceptsIntegerEstimatedHours() {
        TaskGroup group = new TaskGroup();
        group.setId(7L);
        group.setName("Backend");
        when(taskGroupService.findAll()).thenReturn(List.of(group));

        BotActions actions = newActions(1001L, "/addtask Implement login 3");

        assertDoesNotThrow(actions::fnAddTask);
        verify(taskService).createTaskInGroupWithHours(7L, "Implement login", 3f, requesterUser);
    }

    @Test
    void guidedTaskCreationAcceptsIntegerEstimatedHours() {
        long chatId = 1002L;

        BotActions selectGroup = newActions(chatId, "ADDTASKGROUP::7");
        selectGroup.fnSelectGroupForNewTask();

        BotActions enterTitle = newActions(chatId, "Implement login");
        enterTitle.fnCreateTaskFromSelectedGroup();

        when(taskService.getTasksByGroupId(7L, requesterUser)).thenReturn(List.of());
        BotActions enterHours = newActions(chatId, "3");

        assertDoesNotThrow(enterHours::fnCreateTaskFromSelectedGroup);
        verify(taskService).createTaskInGroupWithHours(7L, "Implement login", 3f, requesterUser);
    }

    private BotActions newActions(long chatId, String requestText) {
        BotActions actions = new BotActions(
                telegramClient,
                toDoItemService,
                deepSeekService,
                sprintService,
                taskService,
                taskGroupService,
                userService
        );
        actions.setChatId(chatId);
        actions.setRequestText(requestText);
        actions.setRequesterUser(requesterUser);
        return actions;
    }
}
