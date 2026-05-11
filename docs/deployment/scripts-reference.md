---
sidebar_position: 5
---

# Deployment Scripts Reference

## Scripts Overview

All deployment scripts are located in:
- `scripts/` - Main orchestrators
- `MtdrSpring/backend/` - Backend-specific scripts
- `MtdrSpring/scripts/` - Common utilities

---

## Main Orchestrator Scripts

### scripts/main-setup.sh

**Purpose:** One-time initial setup for OCI deployment

**What it does:**
1. Creates Kubernetes namespace `mtdrworkshop`
2. Creates Docker registry secret for OCIR authentication
3. Prompts for and creates database credentials secret
4. Mounts Oracle wallet files as Kubernetes secret
5. Creates frontend admin credentials secret
6. Optionally runs database initialization

**Usage:**
```bash
./scripts/main-setup.sh
```

**Prompts:**
- Wallet directory path
- Database password
- Frontend admin password

**Environment variables required:**
```bash
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/tenancy/repo
export OCI_REGION=mx-queretaro-1
export TODO_PDB_NAME=MTDR_DB
export UI_USERNAME=frontendadmin
```

---

### scripts/main-deploy.sh

**Purpose:** Deploy application to Kubernetes

**What it does:**
1. Verifies namespace exists
2. Validates all secrets are present
3. Optionally builds Docker image
4. Calls backend deploy script
5. Retrieves and displays public IP
6. Saves deployment state

**Usage:**
```bash
./scripts/main-deploy.sh
```

**Output:**
```
✓ Verifying namespace...
✓ Verifying secrets...
✓ Building Docker image... (optional)
✓ Deploying to Kubernetes...
✓ Waiting for pod running...
✓ Retrieving public IP...
✓ Public IP: 202.10.20.15
```

**State file created:**
```
mtdr-deploy.state
├── PUBLIC_IP=202.10.20.15
├── DEPLOYMENT_DATE=2026-05-10
└── NAMESPACE=mtdrworkshop
```

---

### scripts/main-undeploy.sh

**Purpose:** Clean remove all application resources

**What it does:**
1. Verifies namespace exists
2. Removes backend deployment
3. Removes backend service
4. Waits for pod termination
5. Optionally deletes secrets
6. Optionally deletes namespace
7. Cleans up state file

**Usage:**
```bash
./scripts/main-undeploy.sh
```

**Prompts:**
- Confirm pod deletion (required)
- Delete secrets (optional)
- Delete namespace (optional)

---

## Backend Scripts

### MtdrSpring/backend/build.sh

**Purpose:** Build and push Docker image to registry

**What it does:**
1. Runs Maven clean verify
2. Builds Docker image
3. Tags with registry URL
4. Logs into OCIR
5. Pushes image to registry

**Usage:**
```bash
./backend/build.sh
```

**Required environment:**
```bash
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/tenancy/repo
```

---

### MtdrSpring/backend/deploy.sh

**Purpose:** Deploy backend to Kubernetes

**What it does:**
1. Validates kubectl connection
2. Reads Kubernetes manifest template
3. Substitutes environment variables
4. Applies manifest to cluster
5. Waits for pod to be running
6. Waits for service external IP

**Usage:**
```bash
./backend/deploy.sh
```

**Required environment:**
```bash
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/tenancy/repo
export TODO_PDB_NAME=MTDR_DB
export OCI_REGION=mx-queretaro-1
export UI_USERNAME=frontendadmin
```

**Variables substituted in manifest:**
- `%DOCKER_REGISTRY%` - Container image URL
- `%TODO_PDB_NAME%` - Database name
- `%OCI_REGION%` - OCI region
- `%UI_USERNAME%` - Frontend admin username

---

### MtdrSpring/backend/undeploy.sh

**Purpose:** Remove backend from Kubernetes

**What it does:**
1. Deletes backend deployment
2. Deletes backend service
3. Waits for pods to terminate
4. Verifies successful removal

**Usage:**
```bash
./backend/undeploy.sh

# Or with flag to skip confirmation
SKIP_CONFIRM=true ./backend/undeploy.sh
```

---

## Common Functions Library

### scripts/common.sh

Shared utility functions used by all scripts.

**Logging Functions:**
```bash
ok "Operation succeeded"              # Green ✓
warn "Warning message"                # Yellow ⚠
fail "Error occurred"                 # Red ✗
info "Information"                    # Blue ℹ
print_header "Section Title"          # Bold with underline
print_success "Operation done"        # Green success message
print_info_box "Important info"       # Boxed message
```

**Validation Functions:**
```bash
validate_kubectl              # Verify kubectl installed and configured
validate_required_vars VAR1 VAR2...  # Verify environment variables set
validate_var VAR_NAME          # Check single variable not empty
```

**Kubernetes Operations:**
```bash
create_namespace NAMESPACE              # Create K8s namespace
create_docker_secret NAMESPACE          # Create registry auth secret
create_app_secrets NAMESPACE            # Create app secrets
create_wallet_secret NAMESPACE WALLET_DIR  # Mount wallet files
delete_k8s_services NAMESPACE LABEL     # Delete services by label
delete_k8s_deployments NAMESPACE LABEL  # Delete deployments by label
delete_namespace NAMESPACE              # Delete namespace and contents
```

**Monitoring Functions:**
```bash
wait_for_pod_running NAMESPACE LABEL TIMEOUT   # Wait for pod to start
wait_for_service_external_ip NAMESPACE SERVICE TIMEOUT  # Get public IP
```

**State Management:**
```bash
save_state KEY VALUE              # Save deployment state
load_state KEY                    # Load deployment state
```

---

## Typical Workflow

### First Time Setup

```bash
cd MtdrSpring

# Set environment
export DOCKER_REGISTRY=mx-queretaro-1.ocir.io/tenancy/repo
export OCI_REGION=mx-queretaro-1
export TODO_PDB_NAME=MTDR_DB
export UI_USERNAME=frontendadmin

# Initial setup (creates namespace, secrets, etc.)
./scripts/main-setup.sh

# Deploy application
./scripts/main-deploy.sh

# Get public IP from output
# http://202.10.20.15
```

### Subsequent Deployments

```bash
# Just deploy (namespace and secrets already exist)
./scripts/main-deploy.sh

# Check public IP
cat mtdr-deploy.state
```

### Cleanup

```bash
# Undeploy (remove pods and services, keep namespace)
./scripts/main-undeploy.sh

# Later: redeploy with main-deploy.sh
```

---

## Debugging

### View Script Execution

```bash
# Run with debug output
bash -x scripts/main-deploy.sh
```

### Check Script Syntax

```bash
# Validate script without running
bash -n scripts/main-deploy.sh
```

### Manual Kubernetes Operations

```bash
# Create namespace manually
kubectl create namespace mtdrworkshop

# Create registry secret manually
kubectl create secret docker-registry regcred \
  --docker-server=mx-queretaro-1.ocir.io \
  --docker-username=YOUR_USER \
  --docker-password=YOUR_TOKEN \
  -n mtdrworkshop

# Apply manifest manually
kubectl apply -f src/main/resources/todolistapp-springboot.yaml -n mtdrworkshop
```

---

**Next:** See [Troubleshooting](/docs/deployment/troubleshooting).
