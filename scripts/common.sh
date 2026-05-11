#!/bin/bash
#
# =============================================================================
# MtdrSpring Common Functions Library
# Shared utilities for deploy/undeploy scripts
# =============================================================================

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WAIT]${NC} $1"; }
fail() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }

# =============================================================================
# Validation Functions
# =============================================================================

validate_var() {
    local var_name="$1"
    local var_value="$2"
    local source_method="$3"

    if [ -z "$var_value" ]; then
        fail "Error: $var_name env variable needs to be set! (Tried: $source_method)"
    fi
}

validate_required_vars() {
    local vars_file="$1"
    
    # Source the variables file if provided
    if [ -n "$vars_file" ] && [ -f "$vars_file" ]; then
        set -a
        source "$vars_file"
        set +a
    fi

    # Check required variables
    if [ -z "$DOCKER_REGISTRY" ]; then
        fail "DOCKER_REGISTRY not set"
    fi
    
    if [ -z "$OCI_REGION" ]; then
        fail "OCI_REGION not set"
    fi
    
    if [ -z "$NAMESPACE" ]; then
        export NAMESPACE="mtdrworkshop"
        warn "NAMESPACE not set, using default: $NAMESPACE"
    fi

    ok "All required variables validated"
}

validate_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        fail "kubectl not found. Please install kubectl."
    fi
    
    if ! kubectl cluster-info --request-timeout=10s > /dev/null 2>&1; then
        fail "kubectl cannot reach cluster. Please configure kubectl."
    fi
    
    ok "kubectl configured and cluster reachable"
}

# =============================================================================
# Kubernetes Functions
# =============================================================================

create_namespace() {
    local ns="$1"
    
    if kubectl get namespace "$ns" > /dev/null 2>&1; then
        ok "Namespace '$ns' already exists"
    else
        kubectl create namespace "$ns"
        ok "Namespace '$ns' created"
    fi
}

create_docker_secret() {
    local ns="$1"
    local secret_name="${2:-regcred}"
    
    if kubectl get secret "$secret_name" -n "$ns" > /dev/null 2>&1; then
        ok "Secret '$secret_name' already exists — skipping"
        return 0
    fi
    
    echo -n "Docker registry server (e.g., ocir.io/tenancy): "
    read -r DOCKER_SERVER
    echo -n "Docker username (format: tenancy/username): "
    read -r DOCKER_USER
    echo -n "Docker password/auth token: "
    read -rs DOCKER_PASSWORD
    echo ""
    echo -n "Docker email: "
    read -r DOCKER_EMAIL
    
    kubectl create secret docker-registry "$secret_name" \
        --docker-server="$DOCKER_SERVER" \
        --docker-username="$DOCKER_USER" \
        --docker-password="$DOCKER_PASSWORD" \
        --docker-email="$DOCKER_EMAIL" \
        -n "$ns"
    
    ok "Docker secret '$secret_name' created"
}

create_app_secrets() {
    local ns="$1"
    local secret_name="${2:-mtdr-secrets}"
    
    if kubectl get secret "$secret_name" -n "$ns" > /dev/null 2>&1; then
        ok "Secret '$secret_name' already exists — skipping"
        return 0
    fi
    
    info "Creating application secrets..."
    echo -n "DB_USERNAME: "
    read -r DB_USERNAME
    echo -n "DB_PASSWORD: "
    read -rs DB_PASSWORD
    echo ""
    echo -n "WALLET_TRUSTSTORE_PASSWORD: "
    read -rs WALLET_TS_PASS
    echo ""
    echo -n "WALLET_KEYSTORE_PASSWORD: "
    read -rs WALLET_KS_PASS
    echo ""
    
    kubectl create secret generic "$secret_name" \
        --from-literal=DB_USERNAME="$DB_USERNAME" \
        --from-literal=DB_PASSWORD="$DB_PASSWORD" \
        --from-literal=WALLET_TRUSTSTORE_PASSWORD="$WALLET_TS_PASS" \
        --from-literal=WALLET_KEYSTORE_PASSWORD="$WALLET_KS_PASS" \
        -n "$ns"
    
    ok "Application secrets created"
}

create_wallet_secret() {
    local ns="$1"
    local wallet_dir="$2"
    local secret_name="${3:-wallet-secret}"
    
    if kubectl get secret "$secret_name" -n "$ns" > /dev/null 2>&1; then
        ok "Wallet secret '$secret_name' already exists — skipping"
        return 0
    fi
    
    if [ ! -d "$wallet_dir" ]; then
        fail "Wallet directory '$wallet_dir' not found"
    fi
    
    kubectl create secret generic "$secret_name" \
        --from-file="$wallet_dir" \
        -n "$ns"
    
    ok "Wallet secret '$secret_name' created"
}

# =============================================================================
# Deployment Functions
# =============================================================================

apply_manifest() {
    local manifest="$1"
    local ns="$2"
    
    if [ ! -f "$manifest" ]; then
        fail "Manifest not found at '$manifest'"
    fi
    
    kubectl apply -f "$manifest" -n "$ns"
    ok "Manifest applied"
}

wait_for_pod_running() {
    local ns="$1"
    local app_label="${2:-app=mtdr-backend}"
    local timeout="${3:-300}" # 5 minutes default
    local poll_interval=10
    local attempts=$((timeout / poll_interval))
    
    warn "Waiting for pod to reach Running state (timeout: ${timeout}s)..."
    
    for i in $(seq 1 "$attempts"); do
        STATUS=$(kubectl get pods -n "$ns" -l "$app_label" --no-headers 2>/dev/null | awk '{print $3}' | head -1)
        
        if [ "$STATUS" = "Running" ]; then
            ok "Pod is Running"
            return 0
        fi
        
        if [ "$i" -eq "$attempts" ]; then
            echo ""
            echo "Pod status after ${timeout}s:"
            kubectl get pods -n "$ns" -l "$app_label"
            echo ""
            echo "Pod events:"
            kubectl describe pod -n "$ns" -l "$app_label" | tail -20
            fail "Pod did not reach Running state"
        fi
        
        echo "  Status: ${STATUS:-Pending} (attempt $i/$attempts)..."
        sleep "$poll_interval"
    done
}

wait_for_service_external_ip() {
    local ns="$1"
    local service_name="$2"
    local timeout="${3:-240}" # 4 minutes default
    local poll_interval=10
    local attempts=$((timeout / poll_interval))
    
    echo "[WAIT] Waiting for service external IP (timeout: ${timeout}s)..." >&2
    
    for i in $(seq 1 "$attempts"); do
        EXTERNAL_IP=$(kubectl get svc "$service_name" -n "$ns" --no-headers 2>/dev/null | awk '{print $4}')
        
        if [ -n "$EXTERNAL_IP" ] && [ "$EXTERNAL_IP" != "<pending>" ]; then
            echo "[OK] Load Balancer ready" >&2
            echo "$EXTERNAL_IP"
            return 0
        fi
        
        if [ "$i" -eq "$attempts" ]; then
            fail "Load Balancer IP still pending after ${timeout}s"
        fi
        
        echo "  Still pending (attempt $i/$attempts)..." >&2
        sleep "$poll_interval"
    done
}

# =============================================================================
# Cleanup Functions
# =============================================================================

delete_k8s_services() {
    local ns="$1"
    
    local svc_count=$(kubectl get svc -n "$ns" --no-headers 2>/dev/null | grep -v "^kubernetes" | wc -l || echo 0)
    
    if [ "$svc_count" -gt 0 ]; then
        kubectl delete svc --all -n "$ns" --timeout=60s
        ok "Services deleted — Load Balancer release triggered"
    else
        ok "No services found in namespace — skipping"
    fi
}

delete_k8s_deployments() {
    local ns="$1"
    local app_label="${2:-}"
    
    if [ -n "$app_label" ]; then
        kubectl delete deployments -l "$app_label" -n "$ns" --timeout=60s 2>/dev/null || true
    else
        kubectl delete deployments --all -n "$ns" --timeout=60s 2>/dev/null || true
    fi
    
    ok "Deployments deleted"
}

delete_namespace() {
    local ns="$1"
    local confirm="${2:-false}"
    
    if [ "$confirm" != "true" ]; then
        echo -n "Delete namespace '$ns'? This cannot be undone. Type 'YES' to confirm: "
        read -r CONFIRM
        if [ "$CONFIRM" != "YES" ]; then
            info "Namespace deletion cancelled"
            return 0
        fi
    fi
    
    kubectl delete namespace "$ns" --timeout=120s
    ok "Namespace '$ns' deleted"
}

# =============================================================================
# State Management
# =============================================================================

save_state() {
    local state_file="$1"
    local key="$2"
    local value="$3"
    
    mkdir -p "$(dirname "$state_file")"
    
    # Use associative array if possible (bash 4+)
    if grep -q "^${key}=" "$state_file" 2>/dev/null; then
        sed -i.bak "s|^${key}=.*|${key}=${value}|" "$state_file"
    else
        echo "${key}=${value}" >> "$state_file"
    fi
}

load_state() {
    local state_file="$1"
    local key="$2"
    
    if [ -f "$state_file" ]; then
        grep "^${key}=" "$state_file" | cut -d'=' -f2-
    fi
}

# =============================================================================
# Utility Functions
# =============================================================================

print_header() {
    local title="$1"
    echo ""
    echo "========================================"
    echo "  $title"
    echo "========================================"
    echo ""
}

print_success() {
    local title="$1"
    echo ""
    echo "========================================"
    echo -e "  ${GREEN}$title${NC}"
    echo "========================================"
    echo ""
}

print_info_box() {
    local title="$1"
    shift
    
    echo ""
    echo "========================================"
    echo "  $title"
    echo "========================================"
    while [ $# -gt 0 ]; do
        echo "  $1"
        shift
    done
    echo ""
}

# =============================================================================
# Export functions so they can be used in subshells
# =============================================================================
export -f ok warn fail info
export -f validate_var validate_required_vars validate_kubectl
export -f create_namespace create_docker_secret create_app_secrets create_wallet_secret
export -f apply_manifest wait_for_pod_running wait_for_service_external_ip
export -f delete_k8s_services delete_k8s_deployments delete_namespace
export -f save_state load_state
export -f print_header print_success print_info_box
