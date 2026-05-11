#!/bin/bash
#
# =============================================================================
# MtdrSpring Backend Undeploy Script
# Safely removes K8s deployment and service
# Preserves: Namespace, Database
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_SCRIPT="$(cd "$SCRIPT_DIR/../.." && pwd)/scripts/common.sh"

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
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-todolistapp-springboot-deployment}"
SERVICE_NAME="${SERVICE_NAME:-todolistapp-springboot-service}"
ROUTER_SERVICE_NAME="${ROUTER_SERVICE_NAME:-todolistapp-backend-router}"
APP_LABEL="${APP_LABEL:-app=todolistapp-springboot}"

# =============================================================================
# Step 1: Validate and Warn
# =============================================================================

print_header "MtdrSpring Backend Undeploy"

validate_kubectl

echo ""
echo -e "${YELLOW}[WARN] This will DELETE the backend deployment and service.${NC}"
echo "Your application data and database will NOT be affected."
echo ""
if [ "${SKIP_CONFIRM:-false}" = "true" ]; then
    CONFIRM="YES"
else
    echo -n "Type YES to continue: "
    read -r CONFIRM
fi

if [ "$CONFIRM" != "YES" ]; then
    info "Undeploy cancelled"
    exit 0
fi

# =============================================================================
# Step 2: Delete Services (releases Load Balancer)
# =============================================================================

echo ""
echo "--- Step 1: Delete Services (releases Load Balancer) ---"
kubectl delete service "$SERVICE_NAME" -n "$NAMESPACE" --timeout=60s 2>/dev/null || true
kubectl delete service "$ROUTER_SERVICE_NAME" -n "$NAMESPACE" --timeout=60s 2>/dev/null || true
ok "Backend services removed (or already absent)"

# =============================================================================
# Step 3: Delete Deployments
# =============================================================================

echo ""
echo "--- Step 2: Delete Deployments ---"
kubectl delete deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=60s 2>/dev/null || true
ok "Backend deployment removed (or already absent)"

# =============================================================================
# Step 4: Wait for Pods Termination
# =============================================================================

echo ""
echo "--- Step 3: Wait for Pods Termination ---"

warn "Waiting for pods to be removed (up to 2 min)..."

for i in $(seq 1 12); do
    POD_COUNT=$(kubectl get pods -n "$NAMESPACE" -l "$APP_LABEL" --no-headers 2>/dev/null | wc -l || echo 0)
    
    if [ "$POD_COUNT" -eq 0 ]; then
        ok "All pods terminated"
        break
    fi
    
    if [ "$i" -eq 12 ]; then
        warn "Pods still present after 2 min — they will terminate on their own"
        break
    fi
    
    echo "  Pods remaining: $POD_COUNT (attempt $i/12)..."
    sleep 10
done

# =============================================================================
# Step 5: Verify Removal
# =============================================================================

echo ""
echo "--- Step 4: Verify Removal ---"

REMAINING_SVCS=$(kubectl get svc -n "$NAMESPACE" --no-headers 2>/dev/null | grep -v "^kubernetes" | wc -l || echo 0)
REMAINING_PODS=$(kubectl get pods -n "$NAMESPACE" -l "$APP_LABEL" --no-headers 2>/dev/null | wc -l || echo 0)

if [ "$REMAINING_SVCS" -eq 0 ] && [ "$REMAINING_PODS" -eq 0 ]; then
    ok "All resources removed"
else
    warn "Some resources may still be terminating:"
    info "Services remaining: $REMAINING_SVCS"
    info "Pods remaining: $REMAINING_PODS"
fi

# =============================================================================
# Success Output
# =============================================================================

print_success "MtdrSpring Backend is DOWN"
print_info_box "Cleanup Complete" \
    "Namespace:       $NAMESPACE (preserved)" \
    "Deployment:      REMOVED" \
    "Service:         REMOVED (Load Balancer released)" \
    "Database:        UNTOUCHED" \
    "Secrets:         PRESERVED"

echo ""
