---
sidebar_position: 1
---

# Database Management

## Oracle Wallet Setup

### Download Wallet from OCI

1. **OCI Console:**
   - Database → Autonomous Databases → MTDR_DB
   - DB Connection → Download Client Credentials
   - Save to `MtdrSpring/backend/wallet/`

2. **Extract Wallet:**
   ```bash
   cd MtdrSpring/backend/wallet/
   unzip Wallet_MTDR_DB.zip
   ls -la
   # Should show: tnsnames.ora, sqlnet.ora, ewallet.p12, ...
   ```

3. **Update ojdbc.properties:**
   ```
   oracle.net.ssl_keystore_password=wallet_password
   oracle.net.ssl_truststore_password=wallet_password
   ```

---

## User Management

### Create Application User

**Connect as ADMIN:**
```bash
sqlplus admin@MTDR_DB_tp

-- Create user
CREATE USER todouser IDENTIFIED BY "YourSecurePass123";
GRANT CONNECT, RESOURCE TO todouser;
GRANT UNLIMITED TABLESPACE TO todouser;
```

### Grant Privileges

```sql
-- Basic privileges
GRANT CREATE TABLE TO todouser;
GRANT CREATE SEQUENCE TO todouser;
GRANT CREATE TRIGGER TO todouser;

-- Or all privileges
GRANT ALL PRIVILEGES TO todouser;
```

---

## Backup & Recovery

### Export Data

```bash
# Full export
expdp admin@MTDR_DB_tp \
  directory=data_pump \
  dumpfile=backup_$(date +%Y%m%d).dmp \
  logfile=export.log \
  full=y

# Export specific schema
expdp admin@MTDR_DB_tp \
  directory=data_pump \
  dumpfile=todouser_backup.dmp \
  logfile=export_user.log \
  schemas=todouser
```

### Import Data

```bash
# Full import
impdp admin@MTDR_DB_tp \
  directory=data_pump \
  dumpfile=backup_20260510.dmp \
  logfile=import.log

# Import with conflict handling
impdp admin@MTDR_DB_tp \
  directory=data_pump \
  dumpfile=todouser_backup.dmp \
  logfile=import.log \
  transform=DISABLE_ARCHIVE_LOGGING:Y
```

---

## Monitoring

### Database Health

```sql
-- Connect as admin
sqlplus admin@MTDR_DB_tp

-- Check database status
SELECT * FROM v$instance;

-- Check tablespace usage
SELECT tablespace_name, free_space, total_space 
FROM dba_tablespaces;

-- Check active sessions
SELECT * FROM v$session WHERE status = 'ACTIVE';
```

### Autonomous DB Console

1. OCI Console → Databases → Autonomous Databases → MTDR_DB
2. Performance Hub → View metrics
3. Database Actions → SQL Workshop for queries

---

**Next:** Learn about [Network Configuration](/docs/guides/network-configuration).
