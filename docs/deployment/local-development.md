---
sidebar_position: 2
---

# Local Development

## Single Machine Setup

### Start Backend (H2 Database)

```bash
cd MtdrSpring/backend

# Terminal 1
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

**Output:**
```
Started MyTodoList in 4.234 seconds
2026-05-10 14:30:00 - INFO - Server started on port 8080
```

**Health check:**
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Working Locally

### File Structure

```
Your Development
├── Terminal 1: Backend running on 8080
├── VS Code: Edit code
└── Browser: http://localhost:8080
```

### Making Changes

#### Backend Change

1. Edit `src/main/java/com/springboot/...`
2. Spring Boot auto-reloads (if devtools enabled)
3. Test: `curl http://localhost:8080/api/tasks`

### Database (H2)

View data via H2 console:

```bash
# Browser: http://localhost:8080/h2-console

# Login:
# JDBC URL: jdbc:h2:mem:todo
# User: sa
# Password: (blank)
```

---

## Debug Mode

### Backend Debugging

```bash
# Enable debug logging
export LOG_LEVEL=DEBUG
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run -Dspring-boot.run.arguments="--debug"
```

**Check logs for errors:**
```bash
tail -f target/spring.log
```

---

## Integration Testing

Test both services communicate:

```bash
# 1. Create task via API
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","status":"To Do"}'

# 2. Check H2 database
# Navigate to http://localhost:8080/h2-console
# SELECT * FROM tasks;
```

---

## Stopping Services

```bash
# Backend: Press Ctrl+C in terminal 1
```

---

**Next:** Try [Docker Setup](/docs/deployment/docker-setup) for containerized local testing.
