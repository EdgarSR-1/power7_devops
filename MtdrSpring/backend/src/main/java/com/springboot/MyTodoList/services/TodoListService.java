@Service
@RequiredArgsConstructor
public class TodoListService {

    private final TodoListRepository repository;

    public List<TodoList> findAll() {
        return repository.findAll();
    }

    public TodoList save(TodoList list) {
        return repository.save(list);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}