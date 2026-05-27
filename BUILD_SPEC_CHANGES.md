# ============================================================================
# SNIPPET: Changes to power7_devops/build_spec.yaml
# ============================================================================
# 
# This document shows the recommended changes to integrate P7 Frontend
# build and deployment into the OCI Build Pipeline.
#
# NOTE: All variable references to env files (env.sh, state/HOME_REGION, etc.)
#       are kept as-is for compatibility with existing setup.
# ============================================================================

# STEP 1: REPLACE the current "Build" step with this:
# ============================================================================

  - type: Command
    name: "Build and Push Frontend & Backend"
    command:  |
              # Source environment variables from deployment config
              cd $OCI_PRIMARY_SOURCE_DIR/MtdrSpring
              source env.sh
              
              # Export OCIR credentials for docker login (already done in previous step)
              # Extract region and namespace from state files
              OCIR_REGION=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/HOME_REGION)
              OCIR_NAMESPACE=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/NAMESPACE)
              
              # ===== Build Frontend =====
              echo "=========================================="
              echo "Building P7 Frontend..."
              echo "=========================================="
              cd /workspace/github_P7frontend
              
              # Make build.sh executable
              chmod +x ./build.sh
              
              # Build and push frontend image to OCIR
              # Using the backend public IP (replace with actual IP or DNS)
              BACKEND_PUBLIC_IP="<REPLACE_WITH_BACKEND_PUBLIC_IP>"
              
              ./build.sh \
                --region "$OCIR_REGION" \
                --namespace "$OCIR_NAMESPACE" \
                --repo "equipo52/yv0fi/p7frontend" \
                --tag "latest" \
                --backend-url "http://${BACKEND_PUBLIC_IP}/api"
              
              echo "Frontend image pushed successfully!"
              
              # ===== Build Backend =====
              echo "=========================================="
              echo "Building Spring Boot Backend..."
              echo "=========================================="
              cd $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/backend
              
              chmod +x ./build.sh
              source build.sh


# STEP 2: ADD this new step AFTER the "Build and Push Frontend & Backend" step
# ============================================================================

  - type: Command
    name: "Create OCIR Image Pull Secret"
    command:  |
              # Export kubeconfig path
              export KUBECONFIG=$HOME/.kube/config
              export PATH=$PATH:$HOME/k8s
              
              # Source environment for OCIR credentials
              cd $OCI_PRIMARY_SOURCE_DIR/MtdrSpring
              source env.sh
              
              OCIR_REGION=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/HOME_REGION)
              OCIR_NAMESPACE=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/NAMESPACE)
              OCIR_USER=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/OCIR_USER)
              OCIR_AUTH_TOKEN=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/OCIR_AUTH_TOKEN)
              OCIR_REGISTRY="${OCIR_REGION}.ocir.io"
              
              # Create namespace for frontend if not exists
              kubectl create namespace p7-frontend --dry-run=client -o yaml | kubectl apply -f -
              
              # Create or update docker registry secret for image pull
              kubectl create secret docker-registry oci-registry-secret \
                --docker-server="${OCIR_REGISTRY}" \
                --docker-username="${OCIR_NAMESPACE}/${OCIR_USER}" \
                --docker-password="${OCIR_AUTH_TOKEN}" \
                --docker-email="deployment@example.com" \
                --namespace p7-frontend \
                --dry-run=client -o yaml | kubectl apply -f -
              
              echo "Image pull secret created/updated in p7-frontend namespace"


# STEP 3: ADD this new step to deploy the Frontend to OKE
# ============================================================================

  - type: Command
    name: "Deploy Frontend to OKE"
    command:  |
              export KUBECONFIG=$HOME/.kube/config
              export PATH=$PATH:$HOME/k8s
              
              cd $OCI_PRIMARY_SOURCE_DIR/MtdrSpring
              source env.sh
              
              OCIR_REGION=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/HOME_REGION)
              OCIR_NAMESPACE=$(cat $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/state/NAMESPACE)
              
              # Navigate to frontend manifest
              cd $OCI_PRIMARY_SOURCE_DIR/MtdrSpring/k8s
              
              # Update the manifest with correct image URL
              IMAGE_URL="${OCIR_REGION}.ocir.io/${OCIR_NAMESPACE}/equipo52/yv0fi/p7frontend:latest"
              
              # Apply manifest (creates/updates Deployment, Service, ConfigMap, etc.)
              sed "s|mx-queretaro-1.ocir.io/axdispqrsjop/|${OCIR_REGION}.ocir.io/${OCIR_NAMESPACE}/|g" \
                frontend-manifest.yaml | kubectl apply -f -
              
              echo "Frontend deployment applied to OKE cluster"
              echo ""
              echo "Waiting for deployment to be ready..."
              kubectl rollout status deployment/p7frontend-deployment -n p7-frontend --timeout=5m
              
              echo ""
              echo "Getting LoadBalancer service info..."
              kubectl get svc p7frontend-service -n p7-frontend -o wide


# STEP 4: OPTIONAL - Add this step for verification and diagnostics
# ============================================================================

  - type: Command
    name: "Verify Frontend Deployment"
    ignoreFailure: True
    command:  |
              export KUBECONFIG=$HOME/.kube/config
              export PATH=$PATH:$HOME/k8s
              
              echo "=========================================="
              echo "Frontend Deployment Status"
              echo "=========================================="
              
              kubectl get namespaces
              kubectl get pods -n p7-frontend -o wide
              kubectl get svc -n p7-frontend -o wide
              
              echo ""
              echo "LoadBalancer External IP (wait if <pending>):"
              kubectl get svc p7frontend-service -n p7-frontend \
                --no-headers -o custom-columns=NAME:.metadata.name,EXTERNAL-IP:.status.loadBalancer.ingress[0].ip,PORT:.spec.ports[0].port
              
              echo ""
              echo "Pod logs (last 50 lines):"
              POD=$(kubectl get pods -n p7-frontend -l app=p7frontend -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
              if [[ -n "$POD" ]]; then
                kubectl logs -n p7-frontend "$POD" --tail=50
              else
                echo "No pods found yet (deployment may still be initializing)"
              fi


# ============================================================================
# CONFIGURATION NOTES FOR env.sh OR state/ FILES
# ============================================================================
# 
# Ensure the following are available in your deployment_config.tgz:
#
# state/HOME_REGION           - OCI region code (e.g., mx-queretaro-1)
# state/NAMESPACE             - OCIR namespace (e.g., axdispqrsjop)
# state/OCIR_USER             - OCIR user (e.g., a00839096@tec.mx)
# state/OCIR_AUTH_TOKEN       - OCIR auth token (from OCI console)
# env.sh                       - Sources all above variables
#
# Also update in the build command:
#   BACKEND_PUBLIC_IP="<REPLACE_WITH_BACKEND_PUBLIC_IP>"
#   
#   This should be the public IP or DNS name of the backend LoadBalancer service.
#   Get it from: kubectl get svc todolistapp-springboot-service -o wide
#

# ============================================================================
# MANUAL TESTING (for local development before pipeline)
# ============================================================================
#
# 1. Build and push frontend locally:
#    cd P7frontend
#    ./build.sh --region mx-queretaro-1 --namespace axdispqrsjop \
#               --backend-url http://<BACKEND_IP>/api
#
# 2. Create image pull secret:
#    kubectl create secret docker-registry oci-registry-secret \
#      --docker-server=mx-queretaro-1.ocir.io \
#      --docker-username='axdispqrsjop/<OCIR_USER>' \
#      --docker-password='<AUTH_TOKEN>' \
#      -n p7-frontend
#
# 3. Update frontend manifest with correct image URL and apply:
#    kubectl apply -f power7_devops/MtdrSpring/k8s/frontend-manifest.yaml
#
# 4. Monitor deployment:
#    kubectl rollout status deployment/p7frontend-deployment -n p7-frontend
#    kubectl get svc p7frontend-service -n p7-frontend
#

