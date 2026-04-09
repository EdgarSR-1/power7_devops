@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository repository;

    public GroupMember save(GroupMember member) {
        return repository.save(member);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}