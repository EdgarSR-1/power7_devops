---
sidebar_position: 10
---

# Frequently Asked Questions

## General

### What is MtdrSpring?

MtdrSpring is a full-stack task management application that showcases:
- Modern Spring Boot backend patterns
- DevOps automation with shell scripts
- Cloud deployment on Oracle OCI

### How do I choose between local and OCI deployment?

| Choice | When | Speed | Cost |
|--------|------|-------|------|
| Local (H2) | Testing, development | Instant | Free |
| Docker Local | Integration testing | 2-5 min | Free |
| OCI Kubernetes | Production, demo, cloud | 10-15 min | Pay per hour |

### Do I need Oracle Database?

**For local testing:** No! We use H2 in-memory database.

**For OCI deployment:** Yes, but it's already provisioned in your OCI tenancy.

---

## Deployment

### "Port 8080 already in use"

**Solution:**
```bash
# Find process
lsof -i :8080

# Kill it
kill -9 <PID>

# Or use different port
./mvnw spring-boot:run -Dserver.port=8081
```

### How long does OCI deployment take?

- Setup (first time): 2-3 minutes
- Deploy: 5-10 minutes (waiting for Load Balancer IP)
- Undeploy: 3-5 minutes

### What's the cost of running on OCI?

Depends on resources:
- **Pod (1 vCPU, 2GB RAM):** ~$0.05/hour
- **Load Balancer:** ~$0.01/hour
- **Storage (minimal):** ~$0.01/hour

**Total:** ~$35-50/month if always running. Use `main-undeploy.sh` to stop costs.

---

## Backend

### How do I run tests?

```bash
cd MtdrSpring/backend
./mvnw test
```

### Where's the Spring Boot config?

Main config: `src/main/resources/application.properties`

Profile-specific:
- Local: `application-local.properties` (H2)
- Oracle: `application-oracle.properties` (Autonomous DB)

### How do I add new API endpoints?

1. Create controller in: `src/main/java/com/springboot/controller/`
2. Example:
   ```java
   @RestController
   @RequestMapping("/api/tasks")
   public class TaskController {
       @GetMapping
       public List<Task> getTasks() { ... }
   }
   ```

### What's the database schema?

Check: [Database Guide](/docs/architecture/database)

---

---

## Troubleshooting

### "kubectl cannot reach cluster"

```bash
# Reconfigure kubectl
oci ce cluster create-kubeconfig \
  --cluster-id YOUR_CLUSTER_ID \
  --region YOUR_REGION \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT \
  --overwrite
```

### "Load Balancer IP pending"

Check OCI Console → Networking → Load Balancers to verify it's being created.

Wait up to 4 minutes for the IP to appear.

### "Database connection timeout"

1. Verify wallet files exist: `ls MtdrSpring/backend/wallet/`
2. Check DB is running: OCI Console → Autonomous Databases
3. Verify credentials in `mtdr-secrets` secret

### "Pod stuck in ImagePullBackOff"

**Cause:** Docker image not found or credentials invalid.

**Fix:**
```bash
# Check image exists
docker login mx-queretaro-1.ocir.io
docker images | grep todolistapp

# Rebuild and push
cd MtdrSpring/backend
./build.sh
```

---

## Scripts & Automation

### What does `common.sh` do?

Shared utility functions used by all deploy scripts:
- kubectl validation
- Kubernetes namespace/secret management
- IP retrieval & waiting
- Logging with colors

### Can I customize the scripts?

**Yes!** Edit:
- `scripts/common.sh` - shared functions
- `scripts/main-setup.sh` - initial setup
- `scripts/main-deploy.sh` - deployment
- `MtdrSpring/backend/deploy.sh` - backend-only deploy

### How do I save deployment state?

State is saved to: `mtdr-deploy.state`

Contains:
```ini
DEPLOYMENT_TIME=2026-05-11_14:30:00
EXTERNAL_IP=202.10.20.15
BACKEND_DEPLOYED=true
```

Load it with: `source mtdr-deploy.state`

---

## Getting Help

- 📖 Check the full [Documentation](/docs/intro)
- 🐛 Review [Troubleshooting Guide](/docs/deployment/troubleshooting)
- 🔧 See [Deployment Scripts Reference](/docs/deployment/scripts-reference)
- 💬 Ask on GitHub Issues

---

**Still stuck?** Check the [Architecture Overview](/docs/architecture/overview) to understand the full system.
