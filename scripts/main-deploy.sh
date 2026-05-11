#!/bin/bash
#
# =============================================================================
# MtdrSpring Main Deploy Script
# Quick deployment of backend to OKE
# Uses: env variables, Docker registry, DB secrets
# Outputs: Backend IP for frontend team
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
# Step 1: Validate Prerequisites
# =============================================================================

print_header "MtdrSpring Main Deploy"

validate_kubectl

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" > /dev/null 2>&1; then
    fail "Namespace '$NAMESPACE' does not exist. Run './scripts/main-setup.sh' first."
fi

ok "Namespace '$NAMESPACE' found"

# Check if secrets exist
for secret in "db-wallet-secret" "dbuser" "frontendadmin" "regcred"; do
    if ! kubectl get secret "$secret" -n "$NAMESPACE" > /dev/null 2>&1; then
        warn "Secret '$secret' not found in namespace '$NAMESPACE'"
        warn "Run './scripts/main-setup.sh' to create required secrets"
    fi
done

# =============================================================================
# Step 2: Build Docker Image (Optional)
# =============================================================================

echo ""
echo "--- Step 1: Docker Image ---"

if [ -f "$BACKEND_DIR/build.sh" ]; then
    echo -n "Build and push Docker image? (y/N): "
    read -r BUILD_IMAGE
    
    if [ "$BUILD_IMAGE" = "y" ] || [ "$BUILD_IMAGE" = "Y" ]; then
        warn "Building Docker image..."
        cd "$BACKEND_DIR"
        bash build.sh
        cd "$SCRIPT_DIR"
        ok "Docker image built and pushed"
    else
        info "Docker build skipped (using existing image)"
    fi
else
    info "Docker build script not found"
fi

# =============================================================================
# Step 3: Deploy Backend
# =============================================================================

echo ""
echo "--- Step 2: Deploy Backend ---"

if [ ! -f "$BACKEND_DIR/deploy.sh" ]; then
    fail "Deploy script not found at $BACKEND_DIR/deploy.sh"
fi

warn "Launching backend deployment..."
cd "$BACKEND_DIR"
bash deploy.sh
cd "$SCRIPT_DIR"

# =============================================================================
# Step 4: Retrieve and Display IP
# =============================================================================

echo ""
echo "--- Step 3: Get Service Information ---"

SERVICE_NAME="todolistapp-springboot-service"
EXTERNAL_IP=$(kubectl get svc "$SERVICE_NAME" -n "$NAMESPACE" --no-headers 2>/dev/null | awk '{print $4}')

if [ -n "$EXTERNAL_IP" ] && [ "$EXTERNAL_IP" != "<pending>" ]; then
    ok "External IP obtained: $EXTERNAL_IP"
else
    warn "Load Balancer IP not yet available — checking in a moment..."
    EXTERNAL_IP=$(wait_for_service_external_ip "$NAMESPACE" "$SERVICE_NAME" 240)
fi

# =============================================================================
# Step 5: Save Deployment Info
# =============================================================================

echo ""
echo "--- Step 4: Save Deployment Information ---"

save_state "$STATE_FILE" "BACKEND_IP" "$EXTERNAL_IP"
save_state "$STATE_FILE" "BACKEND_DEPLOYED" "true"
save_state "$STATE_FILE" "DEPLOYMENT_DATE" "$(date '+%Y-%m-%d %H:%M:%S')"

ok "Deployment information saved"

# =============================================================================
# Success Output
# =============================================================================

print_success "Backend Deploy Complete - Ready for Integration!"
print_info_box "Backend Access Details" \
    "External IP:       $EXTERNAL_IP" \
    "API Base:          http://$EXTERNAL_IP/api" \
    "Health Check:      http://$EXTERNAL_IP/actuator/health" \
    "Swagger UI:        http://$EXTERNAL_IP/swagger-ui.html"

echo ""
echo "========================================"
echo "  NEXT STEPS FOR FRONTEND TEAM"
echo "========================================"
echo "1. Update frontend API_BASE_URL: http://$EXTERNAL_IP/api"
echo "2. Build and deploy frontend container"
echo "3. Configure frontend CORS if needed"
echo ""
echo "Configuration file: $STATE_FILE"
echo ""

