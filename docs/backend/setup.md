---
sidebar_position: 1
---

# Backend Setup & Configuration

## Prerequisites

- Java 11+
- Maven 3.8+
- Oracle Wallet files (for cloud)
- Environment variables set

## Local Development Setup

### 1. Clone and Prepare

```bash
cd MtdrSpring/backend

# Copy environment template
cp .env.local.example .env.local

# Edit with your values
nano .env.local
```

### 2. Build with H2 (No Database Needed)

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw clean build
```

This uses H2 in-memory database for testing.

### 3. Run Application

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

**Server starts at:** `http://localhost:8080`

**H2 Console:** `http://localhost:8080/h2-console`

---

## OCI Setup (With Oracle Database)

### 1. Prepare Wallet

Place Oracle wallet files in:

```bash
MtdrSpring/backend/wallet/
```

**Verify files:**
```bash
ls -la wallet/
# Should show: tnsnames.ora, sqlnet.ora, ewallet.p12, etc.
```

### 2. Create .env.local

```bash
cat > .env.local << 'EOF'
SPRING_PROFILES_ACTIVE=oracle
DOCKER_REGISTRY=mx-queretaro-1.ocir.io/YOUR_TENANCY/YOUR_REPO
OCI_REGION=mx-queretaro-1
TODO_PDB_NAME=YOUR_DB_NAME
UI_USERNAME=your_username
DB_USERNAME=TODOUSER
DB_PASSWORD=your_db_password
WALLET_TRUSTSTORE_PASSWORD=wallet_pass
WALLET_KEYSTORE_PASSWORD=wallet_pass
TELEGRAM_BOT_TOKEN=your_bot_token
TELEGRAM_BOT_NAME=your_bot_name
EOF
```

### 3. Test Oracle Connection

```bash
source .env.local

# Build
./mvnw clean verify

# Run (connects to Oracle)
./mvnw spring-boot:run
```

---

## Spring Profiles

### Local Profile (`application-local.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:todo
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.h2.console.enabled=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

**Use for:** Development, testing, CI/CD

### Oracle Profile (`application-oracle.properties`)

```properties
spring.datasource.url=jdbc:oracle:thin:@${TODO_PDB_NAME}_tp?TNS_ADMIN=${TNS_ADMIN}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=${db_user}
spring.datasource.password=${dbpassword}
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```

**Use for:** Production, OCI cloud

---

## Building Docker Image

### Manual Build

```bash
./mvnw clean verify
docker build -f Dockerfile -t todolistapp-springboot:0.1 .
```

### Automated Build (Push to Registry)

```bash
source .env.local
./build.sh
```

This script:
1. Compiles with Maven
2. Builds Docker image
3. Tags with registry URL
4. Pushes to OCIR

---

## Running Tests

```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=TaskControllerTest

# Skip tests (fast build)
./mvnw clean verify -DskipTests
```

---

## Environment Variables Reference

| Variable | Example | Purpose |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | `local` or `oracle` | Which config to load |
| `DB_USERNAME` | `TODOUSER` | Database user |
| `DB_PASSWORD` | `SecurePass123` | Database password |
| `DOCKER_REGISTRY` | `mx-queretaro-1.ocir.io/tenancy/repo` | Container registry |
| `OCI_REGION` | `mx-queretaro-1` | OCI region |
| `TODO_PDB_NAME` | `MTDR_DB` | Database name |
| `TNS_ADMIN` | `/mtdrworkshop/creds` | Wallet path |

---

**Next:** Explore [Backend Structure](/docs/backend/structure) or [API Reference](/docs/backend/api-reference).
