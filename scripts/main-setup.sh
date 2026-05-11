#!/bin/bash
#
# =============================================================================
# MtdrSpring Main Setup Script
# Complete setup of:
# 1. OKE cluster configuration
# 2. Database (via existing utils)
# 3. Kubernetes secrets (Registry, App, Wallet)
# 4. Namespace creation
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
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
DOCKER_SECRET_NAME="${DOCKER_SECRET_NAME:-regcred}"
APP_SECRET_NAME="${APP_SECRET_NAME:-dbuser}"
WALLET_SECRET_NAME="${WALLET_SECRET_NAME:-db-wallet-secret}"
WALLET_DIR="${WALLET_DIR:-$PROJECT_ROOT/MtdrSpring/backend/wallet}"

# =============================================================================
# Step 1: Validate Environment
# =============================================================================

print_header "MtdrSpring Complete Setup"

validate_kubectl

info "Project root: $PROJECT_ROOT"
info "Namespace: $NAMESPACE"

# =============================================================================
# Step 2: Create Namespace
# =============================================================================

echo ""
echo "--- Step 1: Create Namespace ---"
create_namespace "$NAMESPACE"

# =============================================================================
# Step 3: Create Docker Registry Secret
# =============================================================================

echo ""
echo "--- Step 2: Create Docker Registry Secret ---"
create_docker_secret "$NAMESPACE" "$DOCKER_SECRET_NAME"

# =============================================================================
# Step 4: Create Application Secrets
# =============================================================================

echo ""
echo "--- Step 3: Create Application Secrets ---"
create_app_secrets "$NAMESPACE" "$APP_SECRET_NAME"

if kubectl get secret frontendadmin -n "$NAMESPACE" > /dev/null 2>&1; then
    ok "Secret 'frontendadmin' already exists - skipping"
else
    info "Creating frontend admin secret..."
    echo -n "Frontend UI password: "
    read -rs FRONTEND_UI_PASSWORD
    echo ""
    kubectl create secret generic frontendadmin \
        --from-literal=password="$FRONTEND_UI_PASSWORD" \
        -n "$NAMESPACE"
    ok "Secret 'frontendadmin' created"
fi

# =============================================================================
# Step 5: Create Wallet Secret
# =============================================================================

echo ""
echo "--- Step 4: Create Wallet Secret ---"

if [ -d "$WALLET_DIR" ]; then
    create_wallet_secret "$NAMESPACE" "$WALLET_DIR" "$WALLET_SECRET_NAME"
else
    warn "Wallet directory not found at $WALLET_DIR"
    info "Wallet secret creation skipped"
fi

# =============================================================================
# Step 6: Display Secrets Summary
# =============================================================================

echo ""
echo "--- Step 5: Verify Secrets ---"

echo ""
info "Secrets created in namespace '$NAMESPACE':"
kubectl get secrets -n "$NAMESPACE"

# =============================================================================
# Step 7: Optional - Setup Database
# =============================================================================

echo ""
echo "--- Step 6: Database Setup (Optional) ---"

if [ -f "$PROJECT_ROOT/utils/main-setup.sh" ]; then
    echo -n "Run database setup? (y/N): "
    read -r SETUP_DB
    
    if [ "$SETUP_DB" = "y" ] || [ "$SETUP_DB" = "Y" ]; then
        warn "Launching database setup..."
        bash "$PROJECT_ROOT/utils/main-setup.sh"
    else
        info "Database setup skipped"
    fi
else
    info "Database setup script not found at $PROJECT_ROOT/utils/main-setup.sh"
fi

# =============================================================================
# Success Output
# =============================================================================

print_success "MtdrSpring Setup Complete"
print_info_box "Ready for Deployment" \
    "Namespace:          $NAMESPACE" \
    "Docker Secret:      $DOCKER_SECRET_NAME" \
    "App Secrets:        $APP_SECRET_NAME" \
    "Wallet Secret:      $WALLET_SECRET_NAME" \
    "Next step:          ./scripts/main-deploy.sh"

echo ""
echo "Next: Run './scripts/main-deploy.sh' to deploy the backend"
echo ""
