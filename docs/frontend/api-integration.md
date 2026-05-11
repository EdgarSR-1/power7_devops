---
sidebar_position: 4
---

# Frontend API Integration

## Connecting Frontend to Backend

### Environment Configuration

**Local (Development):**
```bash
export REACT_APP_API_URL=http://localhost:8080/api
```

**OCI (Production):**
```bash
export REACT_APP_API_URL=http://202.10.20.15/api
```

Build with:
```bash
npm run build
```

---

## API Communication Patterns

### 1. Fetch with Auth Token

```jsx
async function fetchAPI(endpoint, options = {}) {
  const token = localStorage.getItem('auth_token');
  
  const response = await fetch(
    `${process.env.REACT_APP_API_URL}${endpoint}`,
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
      },
      ...options,
    }
  );
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  
  return response.json();
}
```

### 2. Login and Store Token

```jsx
async function handleLogin(username, password) {
  const data = await fetchAPI('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  
  // Store token
  localStorage.setItem('auth_token', data.token);
  localStorage.setItem('user', JSON.stringify(data.user));
  
  // Redirect
  navigate('/dashboard');
}
```

### 3. Fetch Tasks with Filter

```jsx
async function getTasks(groupId = null) {
  let url = '/tasks';
  if (groupId) url += `?groupId=${groupId}`;
  
  return fetchAPI(url);
}

// Usage
const tasks = await getTasks(1);
```

---

## Error Handling

### Centralized Error Handler

```jsx
async function apiCall(endpoint, options = {}) {
  try {
    const response = await fetch(
      `${process.env.REACT_APP_API_URL}${endpoint}`,
      options
    );
    
    if (response.status === 401) {
      // Unauthorized - clear token
      localStorage.removeItem('auth_token');
      navigate('/login');
      throw new Error('Session expired');
    }
    
    if (response.status === 403) {
      throw new Error('Access denied');
    }
    
    if (response.status === 404) {
      throw new Error('Resource not found');
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Request failed');
    }
    
    return response.json();
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}
```

---

## Real-World Example

### Task Management Flow

```jsx
import { useState, useEffect } from 'react';

export function TaskDashboard() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Load tasks on mount
  useEffect(() => {
    loadTasks();
  }, []);
  
  // Fetch tasks from API
  async function loadTasks() {
    try {
      setLoading(true);
      const data = await fetchAPI('/tasks');
      setTasks(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }
  
  // Create new task
  async function createTask(taskData) {
    try {
      const newTask = await fetchAPI('/tasks', {
        method: 'POST',
        body: JSON.stringify(taskData),
      });
      setTasks([...tasks, newTask]);
    } catch (err) {
      setError(err.message);
    }
  }
  
  // Update task
  async function updateTask(id, updates) {
    try {
      const updated = await fetchAPI(`/tasks/${id}`, {
        method: 'PUT',
        body: JSON.stringify(updates),
      });
      setTasks(tasks.map(t => t.id === id ? updated : t));
    } catch (err) {
      setError(err.message);
    }
  }
  
  // Delete task
  async function deleteTask(id) {
    try {
      await fetchAPI(`/tasks/${id}`, { method: 'DELETE' });
      setTasks(tasks.filter(t => t.id !== id));
    } catch (err) {
      setError(err.message);
    }
  }
  
  if (loading) return <div>Loading tasks...</div>;
  if (error) return <div className="error">{error}</div>;
  
  return (
    <div className="dashboard">
      <h1>Tasks</h1>
      <button onClick={() => createTask({ title: 'New Task' })}>
        + New Task
      </button>
      
      <div className="task-list">
        {tasks.map(task => (
          <div key={task.id} className="task-item">
            <h3>{task.title}</h3>
            <p>{task.description}</p>
            <button onClick={() => updateTask(task.id, { status: 'Completed' })}>
              Complete
            </button>
            <button onClick={() => deleteTask(task.id)}>Delete</button>
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

## CORS Troubleshooting

If you see CORS errors:

1. **Backend must allow frontend origin:**
   ```java
   @Configuration
   public class CorsConfig implements WebMvcConfigurer {
       @Override
       public void addCorsMappings(CorsRegistry registry) {
           registry.addMapping("/api/**")
               .allowedOrigins("http://localhost:3000", "http://frontend-ip")
               .allowedMethods("*")
               .allowCredentials(true);
       }
   }
   ```

2. **Test with curl (no CORS issues):**
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/tasks
   ```

---

**Next:** Start [Deploying](/docs/deployment/overview).
