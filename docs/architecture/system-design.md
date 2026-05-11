---
sidebar_position: 2
---

# System Design

This page explains how all components work together.

## Request Flow Diagram

```mermaid
graph LR
    A[Browser] -->|1. Request tasks| B[Frontend<br/>Nginx Pod]
    B -->|2. HTTP GET /api/tasks| C[Backend<br/>Spring Boot]
    C -->|3. SELECT * FROM TASKS| D[Oracle DB<br/>with Wallet]
    D -->|4. Return rows| C
    C -->|5. JSON response| B
    B -->|6. Render HTML| A
```

## Component Interaction

### Frontend ↔ Backend Communication

**Endpoint Example:**
```bash
GET http://backend-ip/api/tasks
```

**Request:**
```json
{
  "headers": {
    "Authorization": "Bearer eyJhbGciOiJIUzI1NiIs...",
    "Content-Type": "application/json"
  }
}
```

**Response:**
```json
{
  "tasks": [
    {
      "id": 1,
      "title": "Build UI",
      "status": "In Progress",
      "group": "Frontend"
    }
  ]
}
```

### Backend ↔ Database Connection

**Connection String:**
```
jdbc:oracle:thin:@MTDR_DB_tp?TNS_ADMIN=/mtdrworkshop/creds
```

**Authentication:**
- Username: `TODOUSER`
- Password: Stored in K8s Secret `dbuser`
- Wallet: Mounted from Secret `db-wallet-secret`

---

## Network Isolation

```
┌─────────────────────────────────────────┐
│         Internet (Public)               │
│         └─ OCI Load Balancer IP         │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│      OCI VCN Security List              │
│  Allow:                                 │
│    • Ingress: 80, 443 (frontend+backend)│
│    • Egress: DB on private subnet       │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│   OKE Kubernetes Cluster (Private)      │
│   ┌──────────────┐  ┌──────────────┐   │
│   │ Frontend Pod │  │ Backend Pod  │   │
│   └──────┬───────┘  └──────┬───────┘   │
└──────────┼──────────────────┼───────────┘
           │                  │
           └────────┬─────────┘
                    │
┌───────────────────▼──────────────────┐
│  Oracle Autonomous DB (Private VCN)  │
│  ↑ Only reachable from OKE cluster   │
└────────────────────────────────────┘
```

---

## Database Schema

```sql
-- Tasks Table
CREATE TABLE tasks (
    id NUMBER PRIMARY KEY,
    title VARCHAR2(255),
    description VARCHAR2(1000),
    status VARCHAR2(50),        -- "In Progress", "Completed"
    group_id NUMBER,
    assignee VARCHAR2(100),
    created_date TIMESTAMP
);

-- Groups Table
CREATE TABLE groups (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100),
    description VARCHAR2(500)
);

-- Users Table
CREATE TABLE users (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(100) UNIQUE,
    email VARCHAR2(100),
    role VARCHAR2(50)             -- "ADMIN", "USER"
);
```

---

**Next:** Explore the [Backend Structure](/docs/backend/structure) to see how APIs are built.
