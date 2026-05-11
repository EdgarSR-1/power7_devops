---
sidebar_position: 6
---

# Deployment Troubleshooting

## Common Issues

### 1. Namespace Already Exists

**Error:**
```
Error: namespaces "mtdrworkshop" already exists
```

**Solution:**
```bash
# Check if using same namespace
kubectl get ns mtdrworkshop

# Either use existing namespace or delete old one
kubectl delete namespace mtdrworkshop
./scripts/main-setup.sh  # Run setup again
```

---

### 2. Pod CrashLoopBackOff

**Error:**
```
CrashLoopBackOff   0/1   Crash
```

**Diagnosis:**
```bash
# View logs
kubectl logs -n mtdrworkshop todolistapp-springboot-abc123

# Check previous logs (before crash)
kubectl logs -n mtdrworkshop todolistapp-springboot-abc123 --previous

# Describe pod for more details
kubectl describe pod -n mtdrworkshop todolistapp-springboot-abc123
```


**Database connection failed:**
```
Error: Unable to connect to MTDR_DB_tp

Fix: Verify wallet files exist and secret is mounted
kubectl get secret db-wallet-secret -n mtdrworkshop
```

**Out of memory:**
```
Error: OOMKilled (Exit code 137)

Fix: Increase pod memory in deployment
kubectl set resources deployment todolistapp-springboot-deployment \
  -n mtdrworkshop \
  --limits=memory=2Gi --requests=memory=1Gi
```

**Startup timeout:**
```
Error: Application failed to start

Fix: Check logs for what's blocking, increase pod startup time
```

---

### 3. No Public IP / Pending

**Error:**
```
EXTERNAL-IP: <pending>
```

**Diagnosis:**
```bash
# Check service status
kubectl describe svc todolistapp-springboot-service -n mtdrworkshop
# Check service events
kubectl get events -n mtdrworkshop --sort-by='.lastTimestamp'
```

**Solutions:**

```bash
# Wait longer (up to 2 minutes)
kubectl get svc -n mtdrworkshop -w

# If still pending, check load balancer status in OCI Console
# Or recreate service
kubectl delete svc todolistapp-springboot-service -n mtdrworkshop
kubectl apply -f src/main/resources/todolistapp-springboot.yaml -n mtdrworkshop
```

---

### 4. Image Pull Failed

**Error:**
```
Failed to pull image: authentication required
```

**Diagnosis:**
```bash
# Check if secret exists
kubectl get secrets -n mtdrworkshop | grep regcred

# Check secret details
kubectl get secret regcred -n mtdrworkshop -o yaml
```

**Solutions:**

```bash
# Recreate registry secret
kubectl delete secret regcred -n mtdrworkshop
kubectl create secret docker-registry regcred \
  --docker-server=mx-queretaro-1.ocir.io \
  --docker-username=YOUR_USER \
  --docker-password=YOUR_TOKEN \
  -n mtdrworkshop

# Redeploy
kubectl delete pod -n mtdrworkshop <pod-name>
# New pod will pull with updated secret
```

---

### 5. Database Credentials Error

**Error:**
```
ORA-01017: invalid username/password; logon denied
```

**Diagnosis:**
```bash
# Check if dbuser secret exists
kubectl get secret dbuser -n mtdrworkshop

# Verify pod has access to secret
kubectl exec -it -n mtdrworkshop <pod-name> -- \
  env | grep -i db
```

**Solutions:**

```bash
# Update credentials secret
kubectl delete secret dbuser -n mtdrworkshop
kubectl create secret generic dbuser \
  --from-literal=db_user=TODOUSER \
  --from-literal=dbpassword=NEWPASSWORD \
  -n mtdrworkshop

# Redeploy pod
kubectl delete pod -n mtdrworkshop <pod-name>
```

---

### 6. Wallet Files Not Found

**Error:**
```
Error: wallet files not found in /mtdrworkshop/creds
```

**Diagnosis:**
```bash
# Check wallet secret
kubectl get secret db-wallet-secret -n mtdrworkshop -o yaml | head -20

# Check if wallet directory exists in pod
kubectl exec -it -n mtdrworkshop <pod-name> -- ls -la /mtdrworkshop/creds
```

**Solutions:**

```bash
# Recreate wallet secret from files
kubectl delete secret db-wallet-secret -n mtdrworkshop

# From your machine with wallet files
kubectl create secret generic db-wallet-secret \
  --from-file=MtdrSpring/backend/wallet \
  -n mtdrworkshop

# Redeploy
kubectl delete pod -n mtdrworkshop <pod-name>
```

---

### 7. CORS Error in Frontend

**Error:**
```
Access to XMLHttpRequest blocked by CORS policy
```

**Diagnosis:**
```bash
# Check if backend is reachable
curl -i http://<BACKEND_IP>/api/tasks

# Check backend CORS configuration
kubectl exec -it -n mtdrworkshop <pod-name> -- \
  curl http://localhost:8080/api/tasks
```

**Solutions:**

```bash
# Add CORS headers in Spring Boot
# Update src/main/java/com/springboot/config/CorsConfig.java

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")  // Allow all origins for testing
            .allowedMethods("*");
    }
}

# Rebuild and redeploy
./backend/build.sh
./scripts/main-deploy.sh
```

---

### 8. Pod Keeps Restarting

**Error:**
```
RESTARTS: 5
```

**Diagnosis:**
```bash
# Check logs repeatedly
for i in {1..5}; do
  echo "=== Restart $i ==="
  kubectl logs -n mtdrworkshop <pod-name> --all-containers=true
  sleep 2
done
```

**Solutions:**

```bash
# Check for resources exhaustion
kubectl top pod -n mtdrworkshop

# Check for memory leaks
kubectl describe pod -n mtdrworkshop <pod-name> | grep -A 5 "Last State"

# Increase resources
kubectl set resources deployment todolistapp-springboot-deployment \
  -n mtdrworkshop \
  --requests=memory=1Gi,cpu=500m \
  --limits=memory=2Gi,cpu=2000m
```

---

## Advanced Debugging

### Access Pod Shell

```bash
# Get interactive shell
kubectl exec -it -n mtdrworkshop <pod-name> -- bash

# Check running processes
ps aux

# Check network connectivity
curl http://localhost:8080/actuator/health
ping 8.8.8.8

# Check environment variables
env | grep -i db
```

### View Real-time Logs

```bash
# Stream logs from pod
kubectl logs -f -n mtdrworkshop <pod-name>

# Or from all pods with label
kubectl logs -f -n mtdrworkshop -l app=todolistapp-springboot

# View with timestamps
kubectl logs --timestamps=true -n mtdrworkshop <pod-name>
```

### Check Resource Usage

```bash
# CPU and memory per pod
kubectl top pod -n mtdrworkshop

# CPU and memory per node
kubectl top nodes

# Describe resource requests/limits
kubectl describe pod -n mtdrworkshop <pod-name> | grep -A 5 "Limits"
```

---

## Reset Everything

If debugging gets too complex:

```bash
# Remove entire deployment
./scripts/main-undeploy.sh

# Wait 1 minute

# Start fresh
./scripts/main-setup.sh
./scripts/main-deploy.sh
```

---

**Next:** Check [Scripts Reference](/docs/deployment/scripts-reference) for more details.
