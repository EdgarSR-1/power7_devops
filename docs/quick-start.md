---
sidebar_position: 2
---

# Quick Start (5 minutes)

Get the backend running in minutes with this simplified guide.

## Prerequisites

- ✅ kubectl configured (for OCI)
- ✅ Docker installed
- ✅ Maven 3.8+
- ✅ Oracle Wallet files (for OCI only)

## Option 1: Local Backend (No OCI Needed)

### Step 1: Backend (Spring Boot + H2 Database)

```bash
cd MtdrSpring/backend

# Run with H2 in-memory database (no Oracle needed)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Backend will start at: **http://localhost:8080**

**That's it!** Your local app is running.

---

## Option 2: Docker Local (Containerized)

```bash
cd MtdrSpring/backend

# Build and run backend in Docker
./build.sh
```

Backend runs at: **http://localhost:8080**

---

## Option 3: OCI Kubernetes (Full Cloud Deployment)

### Step 1: Setup (One-time)

```bash
cd power7_devops

# Set environment variables
export DOCKER_REGISTRY="mx-queretaro-1.ocir.io/YOUR_TENANCY/YOUR_REPO"
export TODO_PDB_NAME="YOUR_PDB_NAME"
export OCI_REGION="mx-queretaro-1"
export UI_USERNAME="your_username"

# Run setup script (creates namespace, secrets, wallet)
./scripts/main-setup.sh
```

### Step 2: Deploy

```bash
# Deploy backend to OKE (Kubernetes on OCI)
./scripts/main-deploy.sh
```

**Output will show:**
```
External IP: 202.10.20.15
Backend API: http://202.10.20.15/api
```

### Step 3: Cleanup (When Done)

```bash
./scripts/main-undeploy.sh
```

---

## Next Steps

- 📖 [Full Deployment Guide](/docs/deployment/overview)
- 🚨 [Troubleshooting](/docs/deployment/troubleshooting)

---

## Common Issues

**"kubectl not found"**
- Install: `brew install kubectl`
- Configure OCI kubeconfig with `oci ce cluster create-kubeconfig ...`

**"Port 8080 already in use"**
- Change port: `./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"`

**"Database connection failed"**
- Check wallet files exist in `MtdrSpring/backend/wallet/`
- Verify `DB_PASSWORD` is set correctly

More help? → [FAQ](/docs/faq)
