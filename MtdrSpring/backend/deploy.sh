#!/bin/bash
#
# =============================================================================
# MtdrSpring Backend Deploy Script
# Deploys the backend to OKE with:
# - Automatic namespace creation
# - Secret management
# - Pod validation
# - External IP retrieval
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR"
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

# Load from env or use defaults
NAMESPACE="${NAMESPACE:-mtdrworkshop}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-todolistapp-springboot-deployment}"
SERVICE_NAME="${SERVICE_NAME:-todolistapp-springboot-service}"
APP_LABEL="${APP_LABEL:-app=todolistapp-springboot}"
MANIFEST_TEMPLATE="$BACKEND_DIR/src/main/resources/todolistapp-springboot.yaml"
STATE_FILE="${MTDRWORKSHOP_STATE_HOME:-.}/mtdr-deploy.state"

# =============================================================================
# Step 1: Validate Environment
# =============================================================================

print_header "MtdrSpring Backend Deploy"

validate_kubectl

# Validate required variables
if [ -z "$DOCKER_REGISTRY" ]; then
    fail "DOCKER_REGISTRY env variable not set. Example: mx-queretaro-1.ocir.io/mytenancy/mytodolist"
fi

if [ -z "$TODO_PDB_NAME" ]; then
    fail "TODO_PDB_NAME env variable not set"
fi

if [ -z "$OCI_REGION" ]; then
    fail "OCI_REGION env variable not set"
fi

if [ -z "$UI_USERNAME" ]; then
    fail "UI_USERNAME env variable not set"
fi

ok "All required environment variables validated"

# =============================================================================
# Step 2: Create Namespace
# =============================================================================

echo ""
echo "--- Step 1: Create/Verify Namespace ---"
create_namespace "$NAMESPACE"

# =============================================================================
# Step 3: Create/Verify Secrets (Optional - can skip if already exist)
# =============================================================================

echo ""
echo "--- Step 2: Verify Application Secrets ---"
for required_secret in "db-wallet-secret" "dbuser" "frontendadmin"; do
    if kubectl get secret "$required_secret" -n "$NAMESPACE" > /dev/null 2>&1; then
        ok "Secret '$required_secret' found"
    else
        warn "Secret '$required_secret' not found"
        info "Deployment may fail if this secret is required by the manifest"
    fi
done

# =============================================================================
# Step 4: Generate Deployment Manifest
# =============================================================================

echo ""
echo "--- Step 3: Prepare Deployment Manifest ---"

if [ ! -f "$MANIFEST_TEMPLATE" ]; then
    fail "Manifest template not found at $MANIFEST_TEMPLATE"
fi

CURRENTTIME=$(date '+%F_%H:%M:%S')
MANIFEST_FILE="$SCRIPT_DIR/todolistapp-springboot-${CURRENTTIME}.yaml"

cp "$MANIFEST_TEMPLATE" "$MANIFEST_FILE"
ok "Manifest template copied: $MANIFEST_FILE"

# Replace template variables
sed -i.bak "s|%DOCKER_REGISTRY%|${DOCKER_REGISTRY}|g" "$MANIFEST_FILE"
sed -i.bak "s|%TODO_PDB_NAME%|${TODO_PDB_NAME}|g" "$MANIFEST_FILE"
sed -i.bak "s|%OCI_REGION%|${OCI_REGION}|g" "$MANIFEST_FILE"
sed -i.bak "s|%UI_USERNAME%|${UI_USERNAME}|g" "$MANIFEST_FILE"

# Clean up backup files
rm -f "$MANIFEST_FILE.bak"

ok "Manifest variables substituted"

# =============================================================================
# Step 5: Apply Deployment Manifest
# =============================================================================

echo ""
echo "--- Step 4: Apply Deployment Manifest ---"

if kubectl apply -f "$MANIFEST_FILE" -n "$NAMESPACE"; then
    ok "Deployment manifest applied"
else
    fail "Failed to apply deployment manifest"
fi

# =============================================================================
# Step 6: Wait for Pod Running
# =============================================================================

echo ""
echo "--- Step 5: Wait for Pod Running ---"

wait_for_pod_running "$NAMESPACE" "$APP_LABEL" 300

# =============================================================================
# Step 7: Wait for Service External IP
# =============================================================================

echo ""
echo "--- Step 6: Get External IP ---"

EXTERNAL_IP=$(wait_for_service_external_ip "$NAMESPACE" "$SERVICE_NAME" 240)

# =============================================================================
# Step 8: Save State and Display Results
# =============================================================================

echo ""
echo "--- Step 7: Save Deployment State ---"

save_state "$STATE_FILE" "DEPLOYMENT_TIME" "$CURRENTTIME"
save_state "$STATE_FILE" "NAMESPACE" "$NAMESPACE"
save_state "$STATE_FILE" "DEPLOYMENT_NAME" "$DEPLOYMENT_NAME"
save_state "$STATE_FILE" "SERVICE_NAME" "$SERVICE_NAME"
save_state "$STATE_FILE" "EXTERNAL_IP" "$EXTERNAL_IP"
save_state "$STATE_FILE" "MANIFEST_FILE" "$MANIFEST_FILE"

ok "Deployment state saved"

# =============================================================================
# Success Output
# =============================================================================

print_success "MtdrSpring Backend is UP"
print_info_box "Deployment Details" \
    "Namespace:          $NAMESPACE" \
    "Deployment:         $DEPLOYMENT_NAME" \
    "Service:            $SERVICE_NAME" \
    "External IP:        $EXTERNAL_IP" \
    "Manifest:           $MANIFEST_FILE" \
    "State file:         $STATE_FILE"

echo ""
echo "========================================"
echo "  Access Points"
echo "========================================"
echo "  Backend API:       http://$EXTERNAL_IP/api"
echo "  Health Check:      http://$EXTERNAL_IP/actuator/health"
echo "  Swagger UI:        http://$EXTERNAL_IP/swagger-ui.html"
echo ""
echo "Share this IP with the frontend team!"
echo ""
