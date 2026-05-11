---
sidebar_position: 3
---

# Backend API Reference

## Base URL

**Local:** `http://localhost:8080`

**OCI:** `http://<BACKEND_IP>`

---

## Authentication

All endpoints require JWT token in header:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "pass123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## Tasks Endpoints

### Get All Tasks

```http
GET /api/tasks
Authorization: Bearer <token>
```

**Response:** 200 OK
```json
[
  {
    "id": 1,
    "title": "Build UI",
    "status": "In Progress",
    "groupId": 1,
    "assignee": "John",
    "createdDate": "2026-05-10T14:30:00"
  }
]
```

### Get Task by ID

```http
GET /api/tasks/{id}
Authorization: Bearer <token>
```

### Create Task

```http
POST /api/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "New Task",
  "description": "Task description",
  "status": "To Do",
  "groupId": 1
}

Response: 201 Created
{
  "id": 2,
  "title": "New Task",
  ...
}
```

### Update Task

```http
PUT /api/tasks/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Updated Title",
  "status": "Completed"
}

Response: 200 OK
```

### Delete Task

```http
DELETE /api/tasks/{id}
Authorization: Bearer <token>

Response: 204 No Content
```

---

## Groups Endpoints

### Get All Groups

```http
GET /api/groups
Authorization: Bearer <token>
```

### Create Group

```http
POST /api/groups
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Frontend",
  "description": "Frontend development tasks"
}
```

---

## Health Check

**No auth required:**

```http
GET /actuator/health

Response: 200 OK
{
  "status": "UP"
}
```

---

## Error Responses

### 401 Unauthorized

```json
{
  "error": "Invalid token",
  "timestamp": "2026-05-10T14:30:00"
}
```

### 404 Not Found

```json
{
  "error": "Task not found",
  "id": 999
}
```

### 400 Bad Request

```json
{
  "error": "Validation failed",
  "details": {
    "title": "Title is required"
  }
}
```

---

## Testing Endpoints

### Using curl

```bash
# Get token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass123"}' | jq -r '.token')

# Get tasks
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tasks
```

### Using Postman

1. Set base URL: `http://localhost:8080`
2. Login to get token
3. Add `Authorization: Bearer <token>` header
4. Test endpoints

---

**Next:** Learn [Spring Boot Configuration](/docs/backend/spring-boot-config).
