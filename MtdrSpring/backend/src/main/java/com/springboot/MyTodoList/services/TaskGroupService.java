@Service
@RequiredArgsConstructor
public class TaskGroupService {

    private final TaskGroupRepository repository;

    public List<TaskGroup> findAll() {
        return repository.findAll();
    }

    public TaskGroup save(TaskGroup group) {
        return repository.save(group);
    }

    public TaskGroup findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}