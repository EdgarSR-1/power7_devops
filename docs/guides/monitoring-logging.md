---
sidebar_position: 3
---

# Monitoring & Logging

## Application Logs

### Backend Logs (Kubernetes)

```bash
# View logs
kubectl logs -n mtdrworkshop -l app=todolistapp-springboot

# Stream logs
kubectl logs -f -n mtdrworkshop -l app=todolistapp-springboot

# View with timestamps
kubectl logs --timestamps=true -n mtdrworkshop -l app=todolistapp-springboot

# Previous logs (if pod crashed)
kubectl logs -n mtdrworkshop <pod-name> --previous
```

### Configure Log Level

**Spring Boot:**
```properties
# src/main/resources/application.properties
logging.level.root=INFO
logging.level.com.springboot=DEBUG
logging.level.org.springframework.security=DEBUG

# Send to file
logging.file.name=logs.log
logging.file.max-size=10MB
logging.file.max-history=10
```

---

## Database Monitoring

### Query Performance

```sql
-- Active SQL statements
SELECT sql_id, executions, elapsed_time/executions avg_time 
FROM v$sql 
ORDER BY elapsed_time DESC;

-- Slow queries (> 1 second)
SELECT * FROM v$sql 
WHERE elapsed_time/executions > 1000000;
```

### Resource Monitoring

```sql
-- CPU usage
SELECT stat_name, value FROM v$sysstat 
WHERE stat_name IN ('CPU used by this session', 'db cpu');

-- Memory usage
SELECT component, current_size FROM v$sga;

-- Tablespace usage
SELECT tablespace_name, used_space, free_space 
FROM dba_tablespaces;
```

---

## Kubernetes Metrics

### Pod Resource Usage

```bash
# CPU and memory per pod
kubectl top pod -n mtdrworkshop

# Monitor over time
kubectl top pod -n mtdrworkshop --containers

# Node resource usage
kubectl top nodes
```

### Pod Events

```bash
# Recent events
kubectl get events -n mtdrworkshop --sort-by='.lastTimestamp'

# Detailed event info
kubectl describe pod -n mtdrworkshop <pod-name> | grep -A 20 "Events:"
```

---

## Alerting

### OCI Monitoring

**OCI Console:**
1. Monitoring → Alarms
2. Create Alarm
3. Select metric: Compute CPU, Memory, etc.
4. Set threshold: > 80%
5. Notification channel: Email, Slack, etc.

### Example Alarm (CPU)

```bash
oci monitoring alarm create \
  --display-name "High CPU Usage" \
  --metric-name CpuUtilization \
  --namespace oci_computeagent \
  --statistic Average \
  --threshold 80 \
  --comparison-operator GreaterThanOrEqualTo \
  --evaluation-periods 2 \
  --data-points-to-alarm 2 \
  --notification-title "CPU Alert"
```

---

## Health Checks

### Application Health

```bash
# Health endpoint
curl http://localhost:8080/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Configure Custom Health Indicator

```java
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Check database connection
            builder.up()
                .withDetail("database", "Oracle ATP")
                .withDetail("connection", "healthy");
        } catch (Exception e) {
            builder.down()
                .withException(e);
        }
    }
}
```

---

**Next:** Learn [Scaling & Performance](/docs/guides/scaling-performance).
