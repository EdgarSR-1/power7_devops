---
sidebar_position: 1
---

# Deployment Overview

## Three Deployment Options

| Option | Time | Cost | Use Case |
|--------|------|------|----------|
| **Local H2** | 2 min | Free | Development, testing |
| **Docker Local** | 5 min | Free | Integration testing |
| **OCI Kubernetes** | 15 min | $$ | Production |

---

## Option 1: Local H2 (Fastest)

**Best for:** Quick testing, development, CI/CD

### Start Backend

```bash
cd MtdrSpring/backend

SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

---

## Option 2: Docker Local

**Best for:** Testing Docker build, integration testing

### Prepare

```bash
cd MtdrSpring/backend

# Set environment
export DOCKER_REGISTRY=localhost
export SPRING_PROFILES_ACTIVE=local
```

### Build

```bash
./build.sh
```

### Run

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  localhost/todolistapp-springboot:0.1
```

**Test:**
```bash
curl http://localhost:8080/actuator/health
```

---

## Option 3: OCI Kubernetes (Production)

**Best for:** Production, scalability, high availability

### Prerequisites

```bash
# Install OCI CLI
brew install oci-cli

# Configure OCI
oci setup config

# Get kubeconfig
oci ce cluster create-kubeconfig --cluster-id <CLUSTER_ID> --file kubeconfig.yaml
export KUBECONFIG=$(pwd)/kubeconfig.yaml

# Verify
kubectl cluster-info
```

### Setup (One-time)

```bash
cd MtdrSpring

export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/YOUR_TENANCY/YOUR_REPO
export OCI_REGION=mx-queretaro-1
export TODO_PDB_NAME=MTDR_DB
export UI_USERNAME=frontendadmin

./scripts/main-setup.sh
```

This creates:
- Namespace `mtdrworkshop`
- Docker registry secret
- Database credentials secret
- Wallet secret
- Database (optional)

### Deploy

```bash
./scripts/main-deploy.sh
```

**Output:**
```
✓ Deployment created
✓ Waiting for pod...
✓ Public IP: 202.10.20.15
```

**Access:** `http://202.10.20.15`

### Undeploy

```bash
./scripts/main-undeploy.sh
```

---

## Deployment Comparison

| Aspect | Local H2 | Docker Local | OCI Kubernetes |
|--------|----------|--------------|----------------|
| Database | H2 in-memory | H2 in-memory | Oracle Autonomous |
| Persistence | None (RAM) | None | Yes (disk) |
| Scalability | Single process | Single container | Multiple pods |
| Public access | No | No | Yes (public IP) |
| High availability | No | No | Yes (replicas) |
| Cost | Free | Free | $$ (compute + DB) |
| Setup time | 2 min | 5 min | 15 min |

---

## Workflow

### Development

```bash
# 1. Start local services
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run  # Terminal 1

# 2. Make changes, test locally
# 3. Commit to git
```

### Testing

```bash
# 1. Build Docker image
./backend/build.sh

# 2. Run locally
docker run -p 8080:8080 localhost/todolistapp-springboot:0.1
```

### Production Deployment

```bash
# 1. Initialize OCI cluster (one-time)
./scripts/main-setup.sh

# 2. Push Docker image to registry
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/tenancy/repo
./backend/build.sh

# 3. Deploy to Kubernetes
./scripts/main-deploy.sh

# 4. Get public IP and verify
kubectl get svc -n mtdrworkshop
```

---

## Troubleshooting

### Local: Port Already in Use

```bash
# Use different port
PORT=3001 npm start
```

### Docker: Image Not Found

```bash
# Rebuild image
./backend/build.sh

# List images
docker images | grep todolist
```

### Kubernetes: Pod Not Running

```bash
# Check pod status
kubectl get pods -n mtdrworkshop

# View logs
kubectl logs -n mtdrworkshop -l app=todolistapp-springboot

# Describe pod
kubectl describe pod -n mtdrworkshop <pod-name>
```

---

**Next:** Choose your deployment path:
- [Local Development](/docs/deployment/local-development)
- [Docker Setup](/docs/deployment/docker-setup)
- [OCI Deployment](/docs/deployment/oci-deployment)
