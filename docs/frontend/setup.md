---
sidebar_position: 1
---

# Frontend Setup

## Prerequisites

- Node.js 16+
- npm or yarn
- Backend running (for API integration)

---

## Quick Start

### 1. Install Dependencies

```bash
cd MtdrSpring/backend/src/main/frontend

npm install
```

### 2. Start Development Server

```bash
npm start
```

**Opens:** `http://localhost:3000`

### 3. Build for Production

```bash
npm run build
```

**Output:** `build/` directory with static files

---

## Environment Variables

### Local Development

```bash
# Create .env file
cat > .env.local << 'EOF'
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_ENV=development
EOF
```

### Production (OCI)

```bash
# When deploying to OCI, use backend IP:
cat > .env << 'EOF'
REACT_APP_API_URL=http://202.10.20.15/api
REACT_APP_ENV=production
EOF
```

Then rebuild:
```bash
npm run build
```

---

## Project Structure

```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── components/
│   │   ├── TaskList.jsx
│   │   ├── TaskForm.jsx
│   │   ├── GroupFilter.jsx
│   │   └── Dashboard.jsx
│   ├── pages/
│   │   └── App.jsx
│   ├── api/
│   │   └── client.js          # API communication
│   ├── styles/
│   │   └── App.css
│   └── index.js               # Entry point
├── package.json
└── build/                      # Generated on npm run build
```

---

## Common Tasks

### Add New Component

```jsx
// src/components/NewComponent.jsx
import React from 'react';

export function NewComponent() {
  return <div>New Component</div>;
}

export default NewComponent;
```

### Fetch Data from Backend

```jsx
import { useEffect, useState } from 'react';

export function TaskList() {
  const [tasks, setTasks] = useState([]);
  
  useEffect(() => {
    fetch(`${process.env.REACT_APP_API_URL}/tasks`)
      .then(res => res.json())
      .then(setTasks)
      .catch(console.error);
  }, []);
  
  return (
    <ul>
      {tasks.map(task => (
        <li key={task.id}>{task.title}</li>
      ))}
    </ul>
  );
}
```

### Test Components

```bash
npm test
```

---

## Deployment

### Generate Static Files

```bash
npm run build

# Output: build/ contains index.html and all assets
```

### Serve Locally

```bash
npm install -g serve
serve -s build -l 3000
```

### Deploy to OCI

See: [Frontend Deployment](/docs/deployment/overview)

---

## Troubleshooting

### Port 3000 Already in Use

```bash
# Use different port
PORT=3001 npm start
```

### API Connection Failed

```bash
# Check:
# 1. Backend is running
curl http://localhost:8080/actuator/health

# 2. REACT_APP_API_URL is correct
echo $REACT_APP_API_URL

# 3. CORS is enabled in backend
```

### Build Fails

```bash
# Clean and rebuild
rm -rf node_modules package-lock.json
npm install
npm run build
```

---

**Next:** Learn [Frontend Components](/docs/frontend/components).
