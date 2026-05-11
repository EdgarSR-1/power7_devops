---
sidebar_position: 2
---

# Frontend Project Structure

## Directory Layout

```
MtdrSpring/backend/src/main/frontend/
├── public/
│   ├── index.html              # HTML entry point
│   ├── favicon.ico
│   └── manifest.json           # PWA metadata
├── src/
│   ├── components/
│   │   ├── TaskList.jsx        # Display tasks
│   │   ├── TaskForm.jsx        # Create/edit task
│   │   ├── GroupFilter.jsx     # Filter by group
│   │   ├── TaskDetail.jsx      # Task details modal
│   │   └── Navigation.jsx      # Top navigation
│   ├── pages/
│   │   ├── App.jsx             # Main app component
│   │   ├── Dashboard.jsx       # Dashboard page
│   │   ├── Groups.jsx          # Groups page
│   │   └── Settings.jsx        # Settings page
│   ├── api/
│   │   └── client.js           # API calls
│   ├── hooks/
│   │   ├── useTasks.js         # Tasks logic
│   │   └── useAuth.js          # Auth logic
│   ├── styles/
│   │   ├── App.css
│   │   ├── components.css
│   │   └── variables.css       # CSS variables
│   ├── utils/
│   │   ├── storage.js          # LocalStorage helpers
│   │   └── date.js             # Date formatting
│   ├── index.js                # React entry point
│   └── index.css               # Global styles
├── package.json                # Dependencies
├── .env.local                  # Local config
└── build/                      # Compiled output (after npm run build)
```

---

## Component Structure

### Example: TaskList Component

```jsx
// src/components/TaskList.jsx
import React, { useEffect, useState } from 'react';
import { getTasks } from '../api/client';
import '../styles/components.css';

export function TaskList({ filter = null }) {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    loadTasks();
  }, [filter]);
  
  const loadTasks = async () => {
    try {
      setLoading(true);
      const data = await getTasks(filter);
      setTasks(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;
  
  return (
    <div className="task-list">
      {tasks.map(task => (
        <div key={task.id} className="task-item">
          <h3>{task.title}</h3>
          <p>{task.description}</p>
          <span className={`status ${task.status.toLowerCase()}`}>
            {task.status}
          </span>
        </div>
      ))}
    </div>
  );
}
```

---

## API Client

### src/api/client.js

```javascript
const API_URL = process.env.REACT_APP_API_URL;

// Get auth token from localStorage
function getToken() {
  return localStorage.getItem('auth_token');
}

// Fetch with auth header
async function fetchAPI(endpoint, options = {}) {
  const token = getToken();
  
  const response = await fetch(`${API_URL}${endpoint}`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
      ...options.headers,
    },
    ...options,
  });
  
  if (!response.ok) {
    throw new Error(`API error: ${response.statusText}`);
  }
  
  return response.json();
}

// Tasks API
export async function getTasks(filter = null) {
  let url = '/tasks';
  if (filter?.groupId) url += `?groupId=${filter.groupId}`;
  return fetchAPI(url);
}

export async function createTask(task) {
  return fetchAPI('/tasks', {
    method: 'POST',
    body: JSON.stringify(task),
  });
}

export async function updateTask(id, updates) {
  return fetchAPI(`/tasks/${id}`, {
    method: 'PUT',
    body: JSON.stringify(updates),
  });
}

export async function deleteTask(id) {
  return fetchAPI(`/tasks/${id}`, { method: 'DELETE' });
}

// Auth API
export async function login(username, password) {
  const data = await fetchAPI('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  localStorage.setItem('auth_token', data.token);
  return data;
}

export function logout() {
  localStorage.removeItem('auth_token');
}
```

---

## Custom Hooks

### src/hooks/useTasks.js

```javascript
import { useEffect, useState } from 'react';
import { getTasks, createTask, deleteTask } from '../api/client';

export function useTasks() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const refresh = async () => {
    setLoading(true);
    try {
      const data = await getTasks();
      setTasks(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    refresh();
  }, []);
  
  return { tasks, loading, error, refresh };
}
```

---

## State Management

### Using Context API

```jsx
// src/context/AppContext.jsx
import React, { createContext, useState } from 'react';

export const AppContext = createContext();

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [tasks, setTasks] = useState([]);
  
  return (
    <AppContext.Provider value={{ user, setUser, tasks, setTasks }}>
      {children}
    </AppContext.Provider>
  );
}
```

---

## Styling

### CSS Variables

```css
/* src/styles/variables.css */
:root {
  --primary: #007bff;
  --success: #28a745;
  --danger: #dc3545;
  --warning: #ffc107;
  --light-bg: #f8f9fa;
  --border-color: #dee2e6;
}

/* Usage */
.button {
  background-color: var(--primary);
  border-color: var(--border-color);
}
```

---

**Next:** See [API Integration](/docs/frontend/api-integration).
