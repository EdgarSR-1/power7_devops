package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.config.BotProps;
import com.springboot.MyTodoList.service.DeepSeekService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.TaskGroupService;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotActions;
import com.springboot.MyTodoList.util.BotHelper;
import com.springboot.MyTodoList.util.BotMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ToDoItemBotController  implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final Pattern REGISTER_USER_COMMAND_PATTERN = Pattern.compile("^/?registeruser(?:@\\w+)?(?:\\s+.*)?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LOGIN_COMMAND_PATTERN = Pattern.compile("^/?login(?:@\\w+)?(?:\\s+.*)?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern START_COMMAND_PATTERN = Pattern.compile("^/?start(?:@\\w+)?(?:\\s+-d)?\\s*$", Pattern.CASE_INSENSITIVE);

	private static final Logger logger = LoggerFactory.getLogger(ToDoItemBotController.class);
	private ToDoItemService toDoItemService;
	private DeepSeekService deepSeekService;
	private SprintService sprintService;
	private TaskService taskService;
	private TaskGroupService taskGroupService;
	private UserService userService;
	private final TelegramClient telegramClient;
	
	private final BotProps botProps;

	@Value("${telegram.bot.token}")
	private String telegramBotToken;


	@Override
    public String getBotToken() {
		if(telegramBotToken != null && !telegramBotToken.trim().isEmpty()){
        	return telegramBotToken;
		}else{
			return botProps.getToken();
		}
    }


	public ToDoItemBotController(BotProps bp, ToDoItemService tsvc, DeepSeekService ds, SprintService sprintSvc, TaskService taskSvc, TaskGroupService groupSvc, UserService userSvc) {
		this.botProps = bp;
		telegramClient = new OkHttpTelegramClient(getBotToken());
		toDoItemService = tsvc;
		deepSeekService = ds;
		sprintService = sprintSvc;
		taskService = taskSvc;
		taskGroupService = groupSvc;
		userService = userSvc;
	}

	@Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

	@Override
	public void consume(Update update) {

		if (!update.hasMessage()) return;

		long chatId = update.getMessage().getChatId();
		Long telegramUserId = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getId() : null;

		if (update.getMessage().hasContact() && telegramUserId != null) {
			boolean linked = userService.linkTelegramIdentityByPhone(
					telegramUserId,
					chatId,
					update.getMessage().getContact().getPhoneNumber()
			);

			String linkMessage = linked
					? "Phone linked with your Telegram account successfully."
					: "Could not find a user with this phone. Register first with /registeruser Name email@example.com password phone";
			BotHelper.sendMessageToTelegram(chatId, linkMessage, telegramClient);
		}

		if (!update.getMessage().hasText()) return;

		

		String messageTextFromTelegram = update.getMessage().getText();
		String normalizedRequest = messageTextFromTelegram != null ? messageTextFromTelegram.trim() : "";

		Optional<com.springboot.MyTodoList.model.User> requesterUser = userService.findByTelegramUserId(telegramUserId);
		boolean isRegisterFlow = REGISTER_USER_COMMAND_PATTERN.matcher(normalizedRequest).matches();
		boolean isLoginFlow = LOGIN_COMMAND_PATTERN.matcher(normalizedRequest).matches();
		boolean isStartFlow = START_COMMAND_PATTERN.matcher(normalizedRequest).matches();

		logger.info("bot_request chatId={} telegramUserId={} text='{}' requesterFound={}",
				chatId,
				telegramUserId,
				normalizedRequest,
				requesterUser.isPresent());

		if (requesterUser.isEmpty() && !isRegisterFlow && !isLoginFlow && !isStartFlow) {
			BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
			return;
		}

		BotActions actions = new BotActions(telegramClient, toDoItemService, deepSeekService, sprintService, taskService, taskGroupService, userService);
		actions.setRequestText(messageTextFromTelegram);
		actions.setChatId(chatId);
		actions.setTelegramUserId(telegramUserId);
		requesterUser.ifPresent(actions::setRequesterUser);
		if(actions.getTodoService()==null){
			logger.info("todosvc error");
			actions.setTodoService(toDoItemService);
		}

		actions.fnSwitchUser();
		actions.fnStart();
		actions.fnDone();
		actions.fnUndo();
		actions.fnDelete();
		actions.fnHide();
		actions.fnListAll();
		actions.fnListSprints();
		actions.fnSprintTasks();
		actions.fnLogin();
		actions.fnRegisterUser();
		actions.fnAddTask();
		actions.fnCreateSprint();
		actions.fnMoveSprint();
		actions.fnStartTask();
		actions.fnCompleteTask();
		actions.fnListGroups();
		actions.fnListGroupTasks();
		actions.fnTaskDone();
		actions.fnTaskStart();
		actions.fnTaskUndo();
		actions.fnTaskDelete();
		actions.fnSelectGroupForNewTask();
		actions.fnCreateTaskFromSelectedGroup();
		actions.fnCreateGroupPrompt();
		actions.fnCreateGroup();
		actions.fnAddItem();
		actions.fnLLM();
		actions.fnElse();

	}

	@AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }

}

