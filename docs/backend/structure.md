---
sidebar_position: 2
---

# Backend Project Structure

## Directory Layout

```
MtdrSpring/backend/
├── src/
│   ├── main/
│   │   ├── java/com/springboot/
│   │   │   ├── controller/          # REST API endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Database access (JPA)
│   │   │   ├── entity/              # JPA entities (tables)
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── config/              # Spring configurations
│   │   │   ├── security/            # JWT, auth
│   │   │   └── MyTodoList.java      # Main app class
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── application-local.properties
│   │   │   ├── application-oracle.properties
│   │   │   └── wallet/              # Oracle wallet files
│   │   └── frontend/                # React app (old)
│   │       ├── src/
│   │       ├── package.json
│   │       └── build/               # Compiled static files
│   └── test/
│       └── java/com/springboot/     # Unit/integration tests
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Docker build config
├── build.sh                         # Build & push script
├── deploy.sh                        # K8s deploy script
├── undeploy.sh                      # K8s cleanup script
└── wallet/                          # Oracle wallet files
```

---

## Key Packages

### `controller/`

REST API endpoints:

```java
@RestController
@RequestMapping("/api")
public class TaskController {
    @GetMapping("/tasks")
    public List<Task> getTasks() { }
    
    @PostMapping("/tasks")
    public Task createTask(@RequestBody Task task) { }
}
```

### `service/`

Business logic and validation:

```java
@Service
public class TaskService {
    public Task createTask(Task task) {
        // Validate
        // Save to DB
        // Return result
    }
}
```

### `repository/`

Database access using Spring Data JPA:

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(String status);
    List<Task> findByGroupId(Long groupId);
}
```

### `entity/`

JPA annotated classes (map to DB tables):

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "status")
    private String status;
}
```

### `config/`

Spring configurations:

```java
@Configuration
public class SecurityConfig {
    // JWT validation, CORS, etc.
}
```

### `security/`

JWT token handling:

```java
@Component
public class JwtProvider {
    public String generateToken(String username) { }
    public boolean validateToken(String token) { }
}
```

---

## Dependency Injection Flow

```
Spring Context
├── TaskRepository (JPA)
├── TaskService (Uses TaskRepository)
├── TaskController (Uses TaskService)
├── SecurityConfig
├── JwtProvider
└── ...
```

When `TaskController` is instantiated, Spring automatically injects `TaskService`.

---

## Data Access Layer Example

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Auto-implemented by Spring:
    // - findAll()
    // - findById()
    // - save()
    // - delete()
    
    // Custom queries:
    @Query("SELECT t FROM Task t WHERE t.status = :status")
    List<Task> findByStatus(@Param("status") String status);
}
```

---

## Build System (Maven)

### Key Commands

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Build JAR
./mvnw package

# Skip tests (faster)
./mvnw package -DskipTests

# Clean build
./mvnw clean package
```

### pom.xml Sections

```xml
<project>
  <properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
  </properties>
  
  <dependencies>
    <!-- Spring Boot starter -->
    <!-- Database driver -->
    <!-- JWT library -->
  </dependencies>
  
  <build>
    <plugins>
      <plugin>spring-boot-maven-plugin</plugin>
    </plugins>
  </build>
</project>
```

---

**Next:** See [API Reference](/docs/backend/api-reference) to understand endpoints.
