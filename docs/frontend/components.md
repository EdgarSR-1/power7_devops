---
sidebar_position: 3
---

# Frontend Components

## Available Components

### TaskList Component

Displays list of tasks with filtering and sorting.

**Props:**
- `tasks` (array) - List of tasks
- `onSelect` (function) - Callback when task selected
- `loading` (boolean) - Show loading state
- `filter` (object) - Filter criteria

**Usage:**
```jsx
<TaskList 
  tasks={tasks} 
  onSelect={handleTaskSelect}
  loading={isLoading}
  filter={{ status: 'In Progress' }}
/>
```

---

### TaskForm Component

Form to create or edit task.

**Props:**
- `task` (object) - Initial task data (null for create)
- `onSubmit` (function) - Callback on save
- `onCancel` (function) - Callback on cancel
- `groups` (array) - Available groups

**Usage:**
```jsx
<TaskForm
  task={null}
  onSubmit={handleCreate}
  onCancel={handleCancel}
  groups={allGroups}
/>
```

---

### GroupFilter Component

Filter tasks by group.

**Props:**
- `groups` (array) - List of groups
- `selected` (string) - Selected group ID
- `onChange` (function) - Callback on selection change

**Usage:**
```jsx
<GroupFilter
  groups={groups}
  selected={selectedGroupId}
  onChange={handleFilterChange}
/>
```

---

### Dashboard Component

Main dashboard page combining all components.

**Features:**
- Task list with sorting
- Group filter
- Create task form
- Task detail view

**Usage:**
```jsx
import Dashboard from '../pages/Dashboard';

export default function App() {
  return <Dashboard />;
}
```

---

## Shared Utilities

### useAPI Hook

```javascript
import { useState, useEffect } from 'react';

export function useAPI(url, options = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const response = await fetch(url, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            ...options.headers,
          },
          ...options,
        });
        if (!response.ok) throw new Error('API error');
        setData(await response.json());
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, [url]);
  
  return { data, loading, error };
}
```

---

## Best Practices

### 1. Component Composition

```jsx
// DON'T: All in one component
function App() {
  // ... 500 lines of code
}

// DO: Break into smaller components
function Dashboard() {
  return (
    <>
      <Header />
      <Sidebar />
      <MainContent />
      <Footer />
    </>
  );
}
```

### 2. Error Handling

```jsx
function TaskList() {
  const [error, setError] = useState(null);
  
  if (error) {
    return (
      <div className="error-alert">
        <strong>Error:</strong> {error}
        <button onClick={() => setError(null)}>Dismiss</button>
      </div>
    );
  }
  
  return <div>Content...</div>;
}
```

### 3. Loading States

```jsx
function TaskList() {
  const [loading, setLoading] = useState(false);
  
  if (loading) {
    return <Skeleton count={5} />;  // Show skeleton loader
  }
  
  return <div>Content...</div>;
}
```

---

**Next:** Learn [API Integration](/docs/frontend/api-integration).
