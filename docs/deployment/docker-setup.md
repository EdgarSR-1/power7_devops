---
sidebar_position: 3
---

# Docker Setup (Local)

## Building Docker Image

### Prerequisites

```bash
# Install Docker
# macOS: brew install docker docker-compose
# Linux: sudo apt install docker.io
# Windows: Install Docker Desktop

# Verify
docker --version
```

### Build Image

```bash
cd MtdrSpring/backend

# Automated build
./build.sh

# Manual build
docker build -f Dockerfile -t todolistapp-springboot:0.1 .
```

**Output:**
```
[+] Building 45.2s (12/12) FINISHED
=> todolistapp-springboot:0.1
```

### Verify Image

```bash
docker images | grep todolist
# todolistapp-springboot   0.1      abc123def456   2 minutes ago   850MB
```

---

## Running Container Locally

### Quick Start

```bash
docker run -d \
  -p 8080:8080 \
  --name todoapp \
  -e SPRING_PROFILES_ACTIVE=local \
  todolistapp-springboot:0.1
```

### Verify Running

```bash
docker ps
# CONTAINER ID   IMAGE                          STATUS
# abc123def456   todolistapp-springboot:0.1     Up 2 seconds
```

### Test API

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Docker Compose (Backend Only)

### Create docker-compose.yml

```yaml
version: '3.8'

services:
  backend:
    image: todolistapp-springboot:0.1
    container_name: todoapp-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: local
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

```

### Run with Docker Compose

```bash
# Start both services
docker-compose up

# Detached mode (background)
docker-compose up -d
```

### Access Services

- Backend: `http://localhost:8080`

### Stop Services

```bash
docker-compose down
```

---

## Debugging Container

### View Logs

```bash
# Live logs
docker logs -f todoapp

# Last 50 lines
docker logs --tail 50 todoapp

# With timestamps
docker logs -t todoapp
```

### Execute Commands in Container

```bash
# Interactive shell
docker exec -it todoapp /bin/bash

# Run single command
docker exec todoapp curl http://localhost:8080/actuator/health
```

### Inspect Container

```bash
# Container details
docker inspect todoapp

# Resource usage
docker stats todoapp
```

---

## Push to Registry (OCI)

### Login to Registry

```bash
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io
export DOCKER_USER=YOUR_TENANCY/YOUR_USER
export DOCKER_TOKEN=YOUR_AUTH_TOKEN

echo $DOCKER_TOKEN | docker login -u $DOCKER_USER --password-stdin $DOCKER_REGISTRY
```

### Tag Image

```bash
docker tag todolistapp-springboot:0.1 \
  mx-queretaro-1.ocir.io/tenancy/repo/todolistapp-springboot:0.1
```

### Push Image

```bash
docker push mx-queretaro-1.ocir.io/tenancy/repo/todolistapp-springboot:0.1
```

### Verify in Registry

```bash
# OCI Console → Repositories → Check image
oci artifacts container image list --repository-name todolistapp-springboot
```

---

## Clean Up

### Remove Container

```bash
# Stop
docker stop todoapp

# Remove
docker rm todoapp
```

### Remove Image

```bash
docker rmi todolistapp-springboot:0.1
```

### Clean All

```bash
docker system prune -a
```

---

**Next:** Deploy to [OCI Kubernetes](/docs/deployment/oci-deployment).
