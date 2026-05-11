---
sidebar_position: 4
---

# OCI Kubernetes Deployment

## Architecture

```
Internet
  ↓
OCI Load Balancer (Public IP)
  ↓
OCI Container Engine (Kubernetes)
  ├── mtdrworkshop namespace
  ├── todolistapp-springboot pod (replica 1)
  ├── todolistapp-springboot pod (replica 2)
  └── Service (LoadBalancer) → Public IP
  ↓
Oracle Autonomous Database
  └── Tasks, Groups, Users
```

---

## Prerequisites

### 1. OCI Account & Tenancy

- Tenancy: `YOUR_TENANCY`
- Region: `mx-queretaro-1`
- Compartment: `MtdrWorkshop`

### 2. OCI CLI

```bash
# Install
brew install oci-cli

# Configure
oci setup config

# Verify
oci os namespace get
```

### 3. Kubernetes CLI

```bash
# Install
brew install kubectl

# Get kubeconfig
oci ce cluster create-kubeconfig \
  --cluster-id <CLUSTER_OCID> \
  --file ~/kubeconfig.yaml \
  --region mx-queretaro-1

export KUBECONFIG=~/kubeconfig.yaml

# Verify
kubectl cluster-info
```

### 4. Docker Registry Auth

```bash
# Generate auth token
oci auth token request

# Login to registry
docker login -u 'YOUR_TENANCY/YOUR_USER' \
  mx-queretaro-1.ocir.io
```

---

## Deployment Steps

### Step 1: Setup Environment

```bash
cd MtdrSpring

export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/YOUR_TENANCY/YOUR_REPO
export OCI_REGION=mx-queretaro-1
export TODO_PDB_NAME=MTDR_DB
export UI_USERNAME=frontendadmin
export DB_PASSWORD=YourSecurePassword123
export WALLET_TRUSTSTORE_PASSWORD=wallet_pass
export WALLET_KEYSTORE_PASSWORD=wallet_pass
```

### Step 2: Build & Push Docker Image

```bash
cd backend

./build.sh
```

This:
1. Compiles Maven
2. Builds Docker image
3. Pushes to `mx-queretaro-1.ocir.io/...`

### Step 3: Initial Setup (One-time)

```bash
./scripts/main-setup.sh
```

Prompts for:
- Database user password
- Frontend admin password
- Wallet file location

Creates:
- Kubernetes namespace `mtdrworkshop`
- Docker registry secret `regcred`
- Database credentials secret `dbuser`
- Wallet secret `db-wallet-secret`
- Frontend admin secret `frontendadmin`

### Step 4: Deploy Application

```bash
./scripts/main-deploy.sh
```

Output:
```
✓ Verifying secrets...
✓ Deploying backend...
✓ Waiting for pod...
✓ Waiting for public IP... (30s)
✓ Public IP: 202.10.20.15
✓ Access: http://202.10.20.15

State saved to: mtdr-deploy.state
```

### Step 5: Verify Deployment

```bash
# Check pods
kubectl get pods -n mtdrworkshop

# Check service
kubectl get svc -n mtdrworkshop

# View logs
kubectl logs -n mtdrworkshop -l app=todolistapp-springboot

# Test API
curl http://202.10.20.15/actuator/health
```

---

## Managing Deployment

### Scale Replicas

```bash
# Increase to 3 replicas
kubectl scale deployment todolistapp-springboot-deployment \
  --replicas=3 \
  -n mtdrworkshop

# View replicas
kubectl get pods -n mtdrworkshop
```

### Update Image

```bash
# Push new image
docker push mx-queretaro-1.ocir.io/tenancy/repo/todolistapp-springboot:0.2

# Update deployment
kubectl set image deployment/todolistapp-springboot-deployment \
  todolistapp-springboot=mx-queretaro-1.ocir.io/tenancy/repo/todolistapp-springboot:0.2 \
  -n mtdrworkshop
```

### View Logs

```bash
# All pods
kubectl logs -n mtdrworkshop -l app=todolistapp-springboot

# Specific pod
kubectl logs -n mtdrworkshop todolistapp-springboot-deployment-abc123

# Stream logs
kubectl logs -f -n mtdrworkshop -l app=todolistapp-springboot
```

### SSH into Pod

```bash
# Get pod name
kubectl get pods -n mtdrworkshop

# Execute shell
kubectl exec -it -n mtdrworkshop \
  todolistapp-springboot-deployment-abc123 -- /bin/bash

# Run command
kubectl exec -n mtdrworkshop \
  todolistapp-springboot-deployment-abc123 -- curl http://localhost:8080/actuator/health
```

---

## Undeploy

### Remove Application

```bash
./scripts/main-undeploy.sh
```

Prompts to:
1. Delete pod/service (required)
2. Delete secrets (optional)
3. Delete namespace (optional - recommended to keep for redeployment)

---

## Troubleshooting

### Pod not starting

```bash
# Check pod status
kubectl describe pod -n mtdrworkshop <pod-name>

# View events
kubectl get events -n mtdrworkshop --sort-by='.lastTimestamp'

# View logs for startup error
kubectl logs -n mtdrworkshop <pod-name> --previous
```

### No public IP

```bash
# Check service
kubectl describe svc todolistapp-springboot-service -n mtdrworkshop

# Wait longer (up to 2 minutes)
kubectl get svc -n mtdrworkshop -w
```

### Database connection failed

```bash
# Verify wallet secret
kubectl get secret db-wallet-secret -n mtdrworkshop -o yaml | head -20

# Check if pod can reach DB
kubectl exec -it -n mtdrworkshop <pod-name> -- \
  nslookup MTDR_DB_tp
```

### Out of memory

```bash
# Check pod resource usage
kubectl top pod -n mtdrworkshop

# Edit deployment to increase memory
kubectl edit deployment todolistapp-springboot-deployment -n mtdrworkshop

# Change: memory: "512Mi" → memory: "2Gi"
```

---

## Monitoring

### View Metrics

```bash
# Pod CPU/Memory
kubectl top pods -n mtdrworkshop

# Node utilization
kubectl top nodes
```

### View Dashboard

```bash
# Start kubectl proxy
kubectl proxy

# Access: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy
```

---

**Next:** Learn [Scripts Reference](/docs/deployment/scripts-reference).
