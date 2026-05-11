---
sidebar_position: 1
---

# Welcome to MtdrSpring

MtdrSpring is a **full-stack task management application** demonstrating modern DevOps practices with:

- **Spring Boot backend** running on OCI Kubernetes
- **Oracle Autonomous Database** for data persistence
- **Automated deployment scripts** for local and cloud environments

## What You'll Build

A complete task management dashboard where you can:
- ✅ Create and manage tasks
- 👥 Organize tasks by groups
- 🔍 Filter and search tasks
- 📊 View task status and assignments

## Key Technologies

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend | Spring Boot | 3.5.6 |
| Database | Oracle Autonomous | Cloud |
| Container | Docker + Kubernetes | OCI |
| Deploy | Shell Scripts | Bash 4+ |

## Project Structure

```
power7_devops/
├── MtdrSpring/
│   ├── backend/                 # Spring Boot app
│   │   ├── src/
│   │   ├── pom.xml
│   │   ├── deploy.sh           # K8s deploy script
│   │   └── undeploy.sh         # K8s cleanup
│   └── terraform/              # OCI infrastructure
├── scripts/
│   ├── common.sh               # Shared functions
│   ├── main-setup.sh           # Initial setup
│   ├── main-deploy.sh          # Full deploy
│   └── main-undeploy.sh        # Full cleanup
└── docs/                        # This documentation
```

## Quick Navigation

- **New to the project?** → [Quick Start](/docs/quick-start)
- **Want to understand the architecture?** → [System Design](/docs/architecture/overview)
- **Ready to deploy?** → [Deployment Guide](/docs/deployment/overview)
- **Stuck?** → [FAQ](/docs/faq) or [Troubleshooting](/docs/deployment/troubleshooting)

## For Different Roles

### 👨‍💻 Developers
Start with [Backend Setup](/docs/backend/setup) and [API Reference](/docs/backend/api-reference)

### 🚀 DevOps Engineers
Check [Deployment Overview](/docs/deployment/overview) and [Scripts Reference](/docs/deployment/scripts-reference)

### 🏗️ Architects
Review [System Design](/docs/architecture/system-design)

---

**Ready?** Let's start with the [Quick Start Guide](/docs/quick-start)!
