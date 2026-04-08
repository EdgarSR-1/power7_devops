@Entity
@Table(name = "group_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id","user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private TaskGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}