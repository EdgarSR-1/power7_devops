---
sidebar_position: 3
---

# Database Configuration

## Oracle Autonomous Database Setup

### Wallet Files

Your database requires Oracle Wallet files for SSL/TLS authentication:

**Required Files:**
```
MtdrSpring/backend/wallet/
├── tnsnames.ora
├── sqlnet.ora
├── ewallet.p12
├── cwallet.sso
├── keystore.jks
├── truststore.jks
└── ojdbc.properties
```

### Connection Configuration

**Environment Variable:**
```bash
export TNS_ADMIN=/mtdrworkshop/creds
```

**JDBC URL:**
```
jdbc:oracle:thin:@MTDR_DB_tp?TNS_ADMIN=/mtdrworkshop/creds
```

**Spring Property:**
```properties
spring.datasource.url=jdbc:oracle:thin:@MTDR_DB_tp?TNS_ADMIN=/mtdrworkshop/creds
spring.datasource.username=TODOUSER
spring.datasource.password=${DB_PASSWORD}
```

---

## Creating Users & Schemas

### Create Application User

```sql
-- Connect as ADMIN first
sqlplus admin@MTDR_DB_tp

-- Create user
CREATE USER todouser IDENTIFIED BY "YourSecurePassword123!";

-- Grant privileges
GRANT CONNECT, RESOURCE TO todouser;
GRANT UNLIMITED TABLESPACE TO todouser;
GRANT CREATE TABLE TO todouser;
GRANT CREATE VIEW TO todouser;
GRANT CREATE PROCEDURE TO todouser;
```

### Create Tables

JPA/Hibernate auto-creates tables from entities:

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    
    private String title;
    private String status;
    private Long groupId;
}
```

Set `spring.jpa.hibernate.ddl-auto=create` in first run.

---

## Backup & Recovery

### Export Data

```bash
expdp admin@MTDR_DB_tp directory=data_pump dumpfile=backup.dmp logfile=export.log
```

### Import Data

```bash
impdp admin@MTDR_DB_tp directory=data_pump dumpfile=backup.dmp logfile=import.log
```

---

**Next:** See [Backend Setup](/docs/backend/setup) to connect the app.
