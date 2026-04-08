@Service
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskAssignmentRepository repository;

    public TaskAssignment save(TaskAssignment assignment) {
        return repository.save(assignment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}