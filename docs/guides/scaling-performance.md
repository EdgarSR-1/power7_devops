---
sidebar_position: 4
---

# Scaling & Performance

## Horizontal Scaling (Add Replicas)

### Scale Kubernetes Deployment

```bash
# View current replicas
kubectl get deployment -n mtdrworkshop todolistapp-springboot-deployment

# Scale to 3 replicas
kubectl scale deployment todolistapp-springboot-deployment \
  --replicas=3 \
  -n mtdrworkshop

# Verify
kubectl get pods -n mtdrworkshop
# Should show 3 pods running
```

### Auto-Scaling (HPA)

```yaml
# autoscale.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: todoapp-hpa
  namespace: mtdrworkshop
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: todolistapp-springboot-deployment
  minReplicas: 1
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**Apply:**
```bash
kubectl apply -f autoscale.yaml
```

---

## Vertical Scaling (Increase Resources)

### Edit Pod Resources

```bash
# Edit deployment
kubectl edit deployment todolistapp-springboot-deployment -n mtdrworkshop
```

Find and update:
```yaml
resources:
  requests:
    memory: "1Gi"    # Increase from 512Mi
    cpu: "1000m"     # Increase from 500m
  limits:
    memory: "2Gi"    # Increase from 1Gi
    cpu: "2000m"     # Increase from 1000m
```

**Or use command:**
```bash
kubectl set resources deployment todolistapp-springboot-deployment \
  -n mtdrworkshop \
  --requests=memory=1Gi,cpu=1000m \
  --limits=memory=2Gi,cpu=2000m
```

---

## Database Scaling

### Autonomous DB Auto-Scaling

**OCI Console:**
1. Databases → Autonomous Databases → MTDR_DB
2. Administration → Auto Scaling
3. Enable: CPU, Storage, Connections

**Via CLI:**
```bash
oci db autonomous-database update \
  --autonomous-database-id ATP_OCID \
  --auto-scaling-enabled true
```

### Connection Pooling

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

---

## Performance Tuning

### Database Query Optimization

```sql
-- Analyze execution plan
EXPLAIN PLAN FOR SELECT * FROM tasks WHERE status = 'In Progress';
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);

-- Create index on frequently queried column
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_group ON tasks(group_id);
```

### Caching Strategy

```java
@Service
public class TaskService {
    @Cacheable("tasks")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    @CacheEvict(value = "tasks", allEntries = true)
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }
}
```

**Configure cache:**
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
```

---

## Load Testing

### Test with Apache JMeter

```bash
# Install
brew install jmeter

# Create test plan
jmeter -g test-results.jtl -l results.jtl &

# Run from CLI
jmeter -n -t TestPlan.jmx -l results.jtl -j jmeter.log
```

### Simple Load Test

```bash
# Install Apache Bench
brew install httpd

# Test 1000 requests, 10 concurrent
ab -n 1000 -c 10 http://localhost:8080/api/tasks

# Results:
# Requests per second: 150
# Time per request: 66.67ms
```

### Test Backend API

```bash
# Create 100 tasks
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/tasks \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"Task $i\",\"status\":\"To Do\"}"
done

# Measure response time
time curl http://localhost:8080/api/tasks
```

---

## Cost Optimization

### Downscale Non-Production

```bash
# Development: 1 replica
kubectl scale deployment todolistapp-springboot-deployment \
  --replicas=1 \
  -n mtdrworkshop

# Reduce database: 1 OCPU + 20GB storage
```

### Reserved Capacity (OCI)

```bash
# Purchase 1-year compute commitment
# Saves ~30% vs on-demand pricing

oci compute compute-capacity-reservation create \
  --compartment-id COMPARTMENT_OCID \
  --quantity 2 \
  --instance-shape "VM.Standard.E4.Flex"
```

---

**Next:** Learn about [Security Hardening](/docs/guides/security-hardening).
