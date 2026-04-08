package com.springboot.MyTodoList.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.TodoList;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.GroupMemberRepository;
import com.springboot.MyTodoList.repository.TaskAssignmentRepository;
import com.springboot.MyTodoList.repository.TaskGroupRepository;
import com.springboot.MyTodoList.repository.TaskRepository;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.TodoListRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private TaskGroupRepository taskGroupRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ToDoItemRepository toDoItemRepository;

    @Mock
    private TodoListRepository todoListRepository;

    private UserService userService;
    private GroupMemberService groupMemberService;
    private TaskGroupService taskGroupService;
    private TaskAssignmentService taskAssignmentService;
    private TaskService taskService;
    private ToDoItemService toDoItemService;
    private TodoListService todoListService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        groupMemberService = new GroupMemberService(groupMemberRepository);
        taskGroupService = new TaskGroupService(taskGroupRepository);
        taskAssignmentService = new TaskAssignmentService(taskAssignmentRepository);
        taskService = new TaskService(taskRepository);
        toDoItemService = new ToDoItemService(toDoItemRepository);
        todoListService = new TodoListService(todoListRepository);
    }

    @Nested
    class UserServiceTests {

        @Test
        void findAllShouldReturnUsersFromRepository() {
            List<User> users = List.of(buildUser(1L), buildUser(2L));
            when(userRepository.findAll()).thenReturn(users);

            List<User> result = userService.findAll();

            assertThat(result).isEqualTo(users);
        }

        @Test
        void findByIdShouldReturnUserWhenItExists() {
            User user = buildUser(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = userService.findById(1L);

            assertThat(result).isSameAs(user);
        }

        @Test
        void findByIntegerIdShouldDelegateToLongLookup() {
            User user = buildUser(7L);
            when(userRepository.findById(7L)).thenReturn(Optional.of(user));

            User result = userService.findById(7);

            assertThat(result).isSameAs(user);
        }

        @Test
        void findByIdShouldThrowWhenUserDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void saveShouldDelegateToRepository() {
            User user = buildUser(null);
            User savedUser = buildUser(1L);
            when(userRepository.save(user)).thenReturn(savedUser);

            User result = userService.save(user);

            assertThat(result).isSameAs(savedUser);
            verify(userRepository).save(user);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            userService.delete(5L);

            verify(userRepository).deleteById(5L);
        }
    }

    @Nested
    class GroupMemberServiceTests {

        @Test
        void saveShouldDelegateToRepository() {
            GroupMember member = buildGroupMember(1L);
            when(groupMemberRepository.save(member)).thenReturn(member);

            GroupMember result = groupMemberService.save(member);

            assertThat(result).isSameAs(member);
            verify(groupMemberRepository).save(member);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            groupMemberService.delete(3L);

            verify(groupMemberRepository).deleteById(3L);
        }
    }

    @Nested
    class TaskGroupServiceTests {

        @Test
        void findAllShouldReturnAllGroups() {
            List<TaskGroup> groups = List.of(buildTaskGroup(1L), buildTaskGroup(2L));
            when(taskGroupRepository.findAll()).thenReturn(groups);

            List<TaskGroup> result = taskGroupService.findAll();

            assertThat(result).isEqualTo(groups);
        }

        @Test
        void saveShouldDelegateToRepository() {
            TaskGroup group = buildTaskGroup(1L);
            when(taskGroupRepository.save(group)).thenReturn(group);

            TaskGroup result = taskGroupService.save(group);

            assertThat(result).isSameAs(group);
            verify(taskGroupRepository).save(group);
        }

        @Test
        void findByIdShouldReturnGroupWhenItExists() {
            TaskGroup group = buildTaskGroup(9L);
            when(taskGroupRepository.findById(9L)).thenReturn(Optional.of(group));

            TaskGroup result = taskGroupService.findById(9L);

            assertThat(result).isSameAs(group);
        }

        @Test
        void findByIdShouldThrowWhenGroupDoesNotExist() {
            when(taskGroupRepository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskGroupService.findById(9L))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            taskGroupService.delete(4L);

            verify(taskGroupRepository).deleteById(4L);
        }
    }

    @Nested
    class TaskAssignmentServiceTests {

        @Test
        void saveShouldDelegateToRepository() {
            TaskAssignment assignment = buildTaskAssignment(1L);
            when(taskAssignmentRepository.save(assignment)).thenReturn(assignment);

            TaskAssignment result = taskAssignmentService.save(assignment);

            assertThat(result).isSameAs(assignment);
            verify(taskAssignmentRepository).save(assignment);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            taskAssignmentService.delete(8L);

            verify(taskAssignmentRepository).deleteById(8L);
        }
    }

    @Nested
    class TaskServiceTests {

        @Test
        void findAllShouldReturnAllTasks() {
            List<Task> tasks = List.of(buildTask(1L), buildTask(2L));
            when(taskRepository.findAll()).thenReturn(tasks);

            List<Task> result = taskService.findAll();

            assertThat(result).isEqualTo(tasks);
        }

        @Test
        void saveShouldDelegateToRepository() {
            Task task = buildTask(1L);
            when(taskRepository.save(task)).thenReturn(task);

            Task result = taskService.save(task);

            assertThat(result).isSameAs(task);
            verify(taskRepository).save(task);
        }

        @Test
        void findByIdShouldReturnTaskWhenItExists() {
            Task task = buildTask(3L);
            when(taskRepository.findById(3L)).thenReturn(Optional.of(task));

            Task result = taskService.findById(3L);

            assertThat(result).isSameAs(task);
        }

        @Test
        void findByIdShouldThrowWhenTaskDoesNotExist() {
            when(taskRepository.findById(3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.findById(3L))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            taskService.delete(6L);

            verify(taskRepository).deleteById(6L);
        }
    }

    @Nested
    class ToDoItemServiceTests {

        @Test
        void findAllShouldReturnAllItems() {
            List<ToDoItem> items = List.of(buildToDoItem(1), buildToDoItem(2));
            when(toDoItemRepository.findAll()).thenReturn(items);

            List<ToDoItem> result = toDoItemService.findAll();

            assertThat(result).isEqualTo(items);
        }

        @Test
        void getItemByIdShouldReturnOkWhenItemExists() {
            ToDoItem item = buildToDoItem(10);
            when(toDoItemRepository.findById(10)).thenReturn(Optional.of(item));

            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(item);
        }

        @Test
        void getItemByIdShouldReturnNotFoundWhenItemDoesNotExist() {
            when(toDoItemRepository.findById(10)).thenReturn(Optional.empty());

            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }

        @Test
        void getToDoItemByIdShouldReturnItemWhenItExists() {
            ToDoItem item = buildToDoItem(11);
            when(toDoItemRepository.findById(11)).thenReturn(Optional.of(item));

            ToDoItem result = toDoItemService.getToDoItemById(11);

            assertThat(result).isSameAs(item);
        }

        @Test
        void getToDoItemByIdShouldThrowWhenItemDoesNotExist() {
            when(toDoItemRepository.findById(11)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> toDoItemService.getToDoItemById(11))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void addToDoItemShouldDelegateToRepository() {
            ToDoItem item = buildToDoItem(null);
            ToDoItem savedItem = buildToDoItem(12);
            when(toDoItemRepository.save(item)).thenReturn(savedItem);

            ToDoItem result = toDoItemService.addToDoItem(item);

            assertThat(result).isSameAs(savedItem);
            verify(toDoItemRepository).save(item);
        }

        @Test
        void updateToDoItemShouldOverwriteIdAndSave() {
            ToDoItem item = buildToDoItem(null);
            ToDoItem savedItem = buildToDoItem(15);
            when(toDoItemRepository.save(item)).thenReturn(savedItem);

            ToDoItem result = toDoItemService.updateToDoItem(15, item);

            assertThat(ReflectionTestUtils.getField(item, "ID")).isEqualTo(15);
            assertThat(result).isSameAs(savedItem);
            verify(toDoItemRepository).save(item);
        }

        @Test
        void deleteToDoItemShouldReturnFalseWhenItemDoesNotExist() {
            when(toDoItemRepository.existsById(20)).thenReturn(false);

            boolean deleted = toDoItemService.deleteToDoItem(20);

            assertThat(deleted).isFalse();
            verify(toDoItemRepository, never()).deleteById(any(Integer.class));
        }

        @Test
        void deleteToDoItemShouldDeleteAndReturnTrueWhenItemExists() {
            when(toDoItemRepository.existsById(20)).thenReturn(true);

            boolean deleted = toDoItemService.deleteToDoItem(20);

            assertThat(deleted).isTrue();
            verify(toDoItemRepository).deleteById(20);
        }
    }

    @Nested
    class TodoListServiceTests {

        @Test
        void findAllShouldReturnAllLists() {
            List<TodoList> lists = List.of(buildTodoList(1L), buildTodoList(2L));
            when(todoListRepository.findAll()).thenReturn(lists);

            List<TodoList> result = todoListService.findAll();

            assertThat(result).isEqualTo(lists);
        }

        @Test
        void saveShouldDelegateToRepository() {
            TodoList list = buildTodoList(1L);
            when(todoListRepository.save(list)).thenReturn(list);

            TodoList result = todoListService.save(list);

            assertThat(result).isSameAs(list);
            verify(todoListRepository).save(list);
        }

        @Test
        void deleteShouldDelegateToRepository() {
            todoListService.delete(13L);

            verify(todoListRepository).deleteById(13L);
        }
    }

    @Nested
    class DeepSeekServiceTests {

        @Test
        void generateTextShouldReturnPromptWhenConfigurationIsMissing() throws Exception {
            DeepSeekService service = new DeepSeekService("", "");

            String result = service.generateText("texto original");

            assertThat(result).isEqualTo("texto original");
        }

        @Test
        void generateTextShouldReturnParsedContentWhenResponseContainsValidJson() throws Exception {
            DeepSeekService service = new DeepSeekService("https://api.example.com/chat", "secret-key");
            HttpClient httpClient = mock(HttpClient.class);
            HttpResponse<String> response = mockHttpResponse();
            ReflectionTestUtils.setField(service, "httpClient", httpClient);
            ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

            when(response.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"respuesta IA\"}}]}");
            when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(response);

            String result = service.generateText("hola");

            assertThat(result).isEqualTo("respuesta IA");
        }

        @Test
        void generateTextShouldReturnEmptyStringWhenResponseBodyIsBlank() throws Exception {
            DeepSeekService service = new DeepSeekService("https://api.example.com/chat", "secret-key");
            HttpClient httpClient = mock(HttpClient.class);
            HttpResponse<String> response = mockHttpResponse();
            ReflectionTestUtils.setField(service, "httpClient", httpClient);

            when(response.body()).thenReturn("   ");
            when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(response);

            String result = service.generateText("hola");

            assertThat(result).isEmpty();
        }

        @Test
        void generateTextShouldReturnRawBodyWhenJsonCannotBeParsed() throws Exception {
            DeepSeekService service = new DeepSeekService("https://api.example.com/chat", "secret-key");
            HttpClient httpClient = mock(HttpClient.class);
            HttpResponse<String> response = mockHttpResponse();
            ReflectionTestUtils.setField(service, "httpClient", httpClient);
            ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

            when(response.body()).thenReturn("respuesta sin formato json");
            when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(response);

            String result = service.generateText("hola");

            assertThat(result).isEqualTo("respuesta sin formato json");
        }

        @SuppressWarnings("unchecked")
        private HttpResponse<String> mockHttpResponse() {
            return mock(HttpResponse.class);
        }

        @SuppressWarnings("unchecked")
        private HttpResponse.BodyHandler<String> anyBodyHandler() {
            return any(HttpResponse.BodyHandler.class);
        }
    }

    private User buildUser(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "name", "Mario Lopez");
        ReflectionTestUtils.setField(user, "email", "mario" + (id == null ? "" : id) + "@example.com");
        ReflectionTestUtils.setField(user, "password", "secret");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 4, 8, 10, 30));
        return user;
    }

    private TaskGroup buildTaskGroup(Long id) {
        TaskGroup group = new TaskGroup();
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(group, "name", "Grupo " + (id == null ? "nuevo" : id));
        ReflectionTestUtils.setField(group, "createdBy", buildUser(1L));
        ReflectionTestUtils.setField(group, "createdAt", LocalDateTime.of(2026, 4, 8, 11, 0));
        return group;
    }

    private TodoList buildTodoList(Long id) {
        TodoList list = new TodoList();
        ReflectionTestUtils.setField(list, "id", id);
        ReflectionTestUtils.setField(list, "group", buildTaskGroup(1L));
        ReflectionTestUtils.setField(list, "name", "Lista " + (id == null ? "nueva" : id));
        ReflectionTestUtils.setField(list, "createdBy", buildUser(1L));
        ReflectionTestUtils.setField(list, "createdAt", LocalDateTime.of(2026, 4, 8, 12, 0));
        return list;
    }

    private Task buildTask(Long id) {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "list", buildTodoList(1L));
        ReflectionTestUtils.setField(task, "title", "Tarea " + (id == null ? "nueva" : id));
        ReflectionTestUtils.setField(task, "description", "Descripcion");
        ReflectionTestUtils.setField(task, "status", "OPEN");
        ReflectionTestUtils.setField(task, "priority", "HIGH");
        ReflectionTestUtils.setField(task, "dueDate", LocalDateTime.of(2026, 4, 10, 18, 0));
        ReflectionTestUtils.setField(task, "createdBy", buildUser(1L));
        ReflectionTestUtils.setField(task, "createdAt", LocalDateTime.of(2026, 4, 8, 12, 30));
        return task;
    }

    private GroupMember buildGroupMember(Long id) {
        GroupMember member = new GroupMember();
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "group", buildTaskGroup(1L));
        ReflectionTestUtils.setField(member, "user", buildUser(2L));
        ReflectionTestUtils.setField(member, "role", "MEMBER");
        ReflectionTestUtils.setField(member, "joinedAt", LocalDateTime.of(2026, 4, 8, 13, 0));
        return member;
    }

    private TaskAssignment buildTaskAssignment(Long id) {
        TaskAssignment assignment = new TaskAssignment();
        ReflectionTestUtils.setField(assignment, "id", id);
        ReflectionTestUtils.setField(assignment, "task", buildTask(1L));
        ReflectionTestUtils.setField(assignment, "user", buildUser(3L));
        return assignment;
    }

    private ToDoItem buildToDoItem(Integer id) {
        ToDoItem item = new ToDoItem();
        ReflectionTestUtils.setField(item, "ID", id);
        ReflectionTestUtils.setField(item, "description", "Elemento " + (id == null ? "nuevo" : id));
        ReflectionTestUtils.setField(item, "done", false);
        ReflectionTestUtils.setField(item, "creation_ts", OffsetDateTime.parse("2026-04-08T12:00:00Z"));
        return item;
    }
}
