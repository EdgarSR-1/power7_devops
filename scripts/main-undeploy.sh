#!/bin/bash
#
# =============================================================================
# MtdrSpring Main Undeploy Script
# Complete cleanup of backend, but preserves:
# - Namespace
# - Database
# - Secrets (can be reused for next deployment)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/MtdrSpring/backend"
COMMON_SCRIPT="$SCRIPT_DIR/common.sh"

# Source common functions
if [ ! -f "$COMMON_SCRIPT" ]; then
    echo "ERROR: Common script not found at $COMMON_SCRIPT"
    exit 1
fi
source "$COMMON_SCRIPT"

# =============================================================================
# Configuration
# =============================================================================

NAMESPACE="${NAMESPACE:-mtdrworkshop}"
STATE_FILE="${MTDRWORKSHOP_STATE_HOME:-.}/mtdr-deploy.state"

# =============================================================================
# Step 1: Validate and Warn
# =============================================================================

print_header "MtdrSpring Complete Undeploy"

validate_kubectl

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" > /dev/null 2>&1; then
    warn "Namespace '$NAMESPACE' does not exist — nothing to undeploy"
    exit 0
fi

echo ""
echo -e "${YELLOW}[WARN] This will DELETE:${NC}"
echo "  - Backend deployment and pods"
echo "  - Load Balancer and services"
echo ""
echo -e "${GREEN}This WILL PRESERVE:${NC}"
echo "  - Namespace (for next deployment)"
echo "  - Database (untouched)"
echo "  - Secrets (for reuse)"
echo ""
echo -n "Type YES to continue: "
read -r CONFIRM

if [ "$CONFIRM" != "YES" ]; then
    info "Undeploy cancelled"
    exit 0
fi

# =============================================================================
# Step 2: Backend Undeploy
# =============================================================================

echo ""
echo "--- Step 1: Backend Undeploy ---"

if [ ! -f "$BACKEND_DIR/undeploy.sh" ]; then
    fail "Undeploy script not found at $BACKEND_DIR/undeploy.sh"
fi

# Skip confirmation in undeploy.sh since we already confirmed
export SKIP_CONFIRM=true

warn "Launching backend undeploy..."
cd "$BACKEND_DIR"
SKIP_CONFIRM=true bash undeploy.sh || true
cd "$SCRIPT_DIR"

ok "Backend undeploy completed"

# =============================================================================
# Step 3: Update State
# =============================================================================

echo ""
echo "--- Step 2: Update Deployment State ---"

save_state "$STATE_FILE" "BACKEND_DEPLOYED" "false"
save_state "$STATE_FILE" "UNDEPLOY_DATE" "$(date '+%Y-%m-%d %H:%M:%S')"

ok "State updated"

# =============================================================================
# Step 4: Verify
# =============================================================================

echo ""
echo "--- Step 3: Verify Cleanup ---"

# Check remaining resources
REMAINING_PODS=$(kubectl get pods -n "$NAMESPACE" -l "app=todolistapp-springboot" --no-headers 2>/dev/null | wc -l || echo 0)
REMAINING_SVCS=$(kubectl get svc -n "$NAMESPACE" --no-headers 2>/dev/null | grep -v "^kubernetes" | wc -l || echo 0)

info "Pods remaining: $REMAINING_PODS"
info "Services remaining: $REMAINING_SVCS"

# =============================================================================
# Step 5: Optional Cleanup
# =============================================================================

echo ""
echo "--- Step 4: Additional Options ---"

echo -n "Delete application secrets (dbuser, frontendadmin, db-wallet-secret)? (y/N): "
read -r DELETE_SECRETS

if [ "$DELETE_SECRETS" = "y" ] || [ "$DELETE_SECRETS" = "Y" ]; then
    kubectl delete secret dbuser -n "$NAMESPACE" 2>/dev/null || true
    kubectl delete secret frontendadmin -n "$NAMESPACE" 2>/dev/null || true
    kubectl delete secret db-wallet-secret -n "$NAMESPACE" 2>/dev/null || true
    ok "Application secrets deleted"
else
    info "Application secrets preserved for next deployment"
fi

echo -n "Delete the entire namespace '$NAMESPACE'? (y/N): "
read -r DELETE_NS

if [ "$DELETE_NS" = "y" ] || [ "$DELETE_NS" = "Y" ]; then
    warn "Deleting namespace $NAMESPACE..."
    kubectl delete namespace "$NAMESPACE" --timeout=120s
    ok "Namespace deleted"
    save_state "$STATE_FILE" "NAMESPACE_DELETED" "true"
else
    info "Namespace preserved for next deployment"
fi

# =============================================================================
# Success Output
# =============================================================================

print_success "Undeploy Complete"
print_info_box "Cleanup Summary" \
    "Backend:           REMOVED" \
    "Load Balancer:     RELEASED" \
    "Database:          UNTOUCHED" \
    "Secrets:           $([ "$DELETE_SECRETS" = "y" ] || [ "$DELETE_SECRETS" = "Y" ] && echo 'DELETED' || echo 'PRESERVED')" \
    "Namespace:         $([ "$DELETE_NS" = "y" ] || [ "$DELETE_NS" = "Y" ] && echo 'DELETED' || echo 'PRESERVED')"

echo ""
echo "Cost savings: Load Balancer and compute resources released"
echo ""
if [ "$DELETE_NS" != "y" ] && [ "$DELETE_NS" != "Y" ]; then
    echo "To redeploy: ./scripts/main-deploy.sh"
else
    echo "To redeploy from scratch: ./scripts/main-setup.sh"
fi
echo ""

