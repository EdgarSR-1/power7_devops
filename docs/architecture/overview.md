---
sidebar_position: 1
---

# System Design Overview

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     OCI LOAD BALANCER (IP Public)           │
└──────┬──────────────────────────────────────────────────────┘
       │
       ├─────────────────┬─────────────────┐
       │                 │                 │
       ▼                 ▼                 ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
  │   Frontend   │ │   Backend    │ │   Backend    │
  │   Pod (Nginx)│ │   Pod 1      │ │   Pod 2      │
  │  Static Site │ │  Port 8080   │ │  Port 8080   │
  └──────────────┘ └──────────────┘ └──────────────┘
       │                 │                 │
       └─────────────────┴─────────────────┘
                         │
                         ▼
            ┌──────────────────────────┐
            │  Spring Boot Application │
            │  • REST API /api/*       │
            │  • JPA/Hibernate         │
            │  • Spring Security       │
            └──────────────────────────┘
                         │
                         ▼
            ┌──────────────────────────┐
            │  Oracle JDBC Driver      │
            │  + Wallet Authentication │
            └──────────────────────────┘
                         │
                         ▼
            ┌──────────────────────────┐
            │ Oracle Autonomous DB     │
            │  • Tasks table           │
            │  • Groups table          │
            │  • User data             │
            └──────────────────────────┘
```

## Components Breakdown

### 1. **Frontend (Kubernetes Pod + Nginx)**

- **Framework:** React or Next.js
- **Serving:** Static files via Nginx
- **Port:** 80 (exposed via Service LoadBalancer)
- **API Communication:** HTTP to Backend IP:8080/api
- **Deployment:** Docker image with Nginx

### 2. **Backend (Spring Boot on Kubernetes)**

- **Framework:** Spring Boot 3.5.6
- **REST API:** Port 8080
- **Database:** Oracle Autonomous DB via JDBC
- **Authentication:** JWT tokens
- **Deployment:** Docker container, managed by Kubernetes

### 3. **Database (Oracle Autonomous)**

- **Type:** Autonomous Transaction Processing (ATP)
- **Authentication:** SSL/TLS with Oracle Wallet
- **Connection:** JDBC URL with TNS_ADMIN path
- **Schema:** Task/Group/User tables created by JPA

### 4. **Kubernetes Orchestration (OCI Container Engine)**

- **Namespaces:** `mtdrworkshop`
- **Services:** LoadBalancer type for public IP
- **Secrets:** Credentials, wallet files, API tokens
- **Deployments:** Replicas for high availability

---

## Data Flow

### User Creates a Task

```
1. Frontend (React) → User fills form → Click Save
2. HTTP POST to Backend → http://<IP>/api/tasks
3. Spring Controller processes request
4. JPA saves to Oracle DB
5. Response JSON returned to Frontend
6. Frontend updates UI with new task
```

### User Views Dashboard

```
1. Frontend loads → HTTP GET http://<IP>/api/tasks
2. Backend queries Oracle via JDBC
3. Returns list of tasks + groups
4. React renders dashboard
5. User sees tasks organized by group
```

---

## Security Layers

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Network | OCI Load Balancer | DDoS protection, IP routing |
| Application | Spring Security + JWT | API authentication, authorization |
| Database | Oracle Wallet (mTLS) | SSL/TLS connection encryption |
| Container | Kubernetes Secrets | Credentials not in images |
| Infrastructure | OCI VCN/Security Lists | Network isolation |

---

## Deployment Topology

### Local (Development)

```
Your Computer
├── Backend (localhost:8080)
│   └── H2 Database (in-memory)
└── Frontend (localhost:3000)
    └── React dev server
```

### OCI (Production)

```
OCI Tenancy
├── VCN (Virtual Cloud Network)
├── OKE Cluster (Kubernetes)
│   ├── Namespace: mtdrworkshop
│   ├── Deployment: backend
│   ├── Service: LoadBalancer (Public IP)
│   └── Secrets: credentials, wallet
└── Autonomous DB (Private endpoint)
    └── Tasks data
```

---

## Key Design Decisions

| Decision | Reason |
|----------|--------|
| Spring Boot | Mature, robust Java framework |
| React/Next.js | Modern UI, easy to maintain |
| Kubernetes | Scalable, industry standard |
| Oracle Autonomous | Managed DB, reduced ops burden |
| Shell scripts for deploy | Simple, portable, no external deps |
| Docker for containers | Reproducible environments |

---

## Scaling Considerations

### Horizontal Scaling

```bash
# Increase backend replicas
kubectl scale deployment todolistapp-springboot-deployment --replicas=3 -n mtdrworkshop
```

### Vertical Scaling

```bash
# Increase pod resources (edit manifest)
# Change: memory: "512Mi" → memory: "2Gi"
# Change: cpu: "500m" → cpu: "2000m"
```

### Database Scaling

Autonomous DB scales automatically. Monitor CPU/memory in OCI Console.

---

**Next:** Learn about the [Backend Implementation](/docs/backend/structure) or dive into [Deployment](/docs/deployment/overview).
