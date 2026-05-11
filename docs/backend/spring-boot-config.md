---
sidebar_position: 4
---

# Spring Boot Configuration

## Application Properties

Located in: `src/main/resources/application.properties`

### Server Configuration

```properties
# Port
server.port=8080

# Context path
server.servlet.context-path=/

# Shutdown
server.shutdown=graceful
server.tomcat.threads.max=200
```

### JPA/Hibernate

```properties
# Show SQL
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# DDL Auto (create | update | validate | none)
spring.jpa.hibernate.ddl-auto=update

# Dialect (auto-detected or explicit)
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```

### Logging

```properties
logging.level.root=INFO
logging.level.org.springframework.security=DEBUG
logging.level.com.springboot=DEBUG
logging.file.name=logs.log
```

### JWT

```properties
jwt.secret=yourSuperSecretKeyThatMustBeAtLeast32CharactersLong123
jwt.expiration=86400000  # 24 hours in milliseconds
```

### External APIs

```properties
deepseek.api.key=${DEEPSEEK_API_KEY:}
deepseek.api.url=https://api.deepseek.com/v1/chat/completions

telegram.bot.token=${TELEGRAM_BOT_TOKEN:}
telegram.bot.name=${TELEGRAM_BOT_NAME:}
```

---

## Spring Profiles

### Local Profile

Use: `application-local.properties`

```properties
# H2 Database
spring.datasource.url=jdbc:h2:mem:todo;MODE=Oracle
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Auto DDL
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
```

**Activate:**
```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### Oracle Profile

Use: `application-oracle.properties`

```properties
# Oracle Autonomous Database
spring.datasource.url=jdbc:oracle:thin:@${TODO_PDB_NAME}_tp?TNS_ADMIN=${TNS_ADMIN}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=${db_user}
spring.datasource.password=${dbpassword}

# No auto DDL on production
spring.jpa.hibernate.ddl-auto=validate
```

**Activate:**
```bash
SPRING_PROFILES_ACTIVE=oracle ./mvnw spring-boot:run
```

---

## Environment Variables

Pass via system environment:

```bash
export db_user=TODOUSER
export dbpassword=MySecurePassword123
export TODO_PDB_NAME=MTDR_DB
export TNS_ADMIN=/path/to/wallet
export JWT_SECRET=your-secret-key-32-chars-min
```

Or in Docker:

```dockerfile
ENV db_user=TODOUSER
ENV dbpassword=${DB_PASSWORD}
```

---

## Actuator Endpoints

Spring Boot Actuator provides monitoring:

```bash
# Health check
GET /actuator/health → {"status":"UP"}

# Metrics
GET /actuator/metrics

# Environment info
GET /actuator/env
```

### Enable/Disable Endpoints

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoints.web.exposure.exclude=env,shutdown
management.endpoint.health.show-details=when-authorized
```

---

## CORS Configuration

Allow external clients to call backend:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
          .allowedOrigins("http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## Logging Configuration

### Logback XML

Create: `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```

---

**Next:** Check [Deployment Overview](/docs/deployment/overview).
