---
sidebar_position: 5
---

# Security Hardening

## Application Security

### JWT Token Security

**Generate strong secret:**
```bash
# Minimum 32 characters
openssl rand -base64 32
# Output: aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890==
```

**Configure in application.properties:**
```properties
jwt.secret=aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890==
jwt.expiration=3600000  # 1 hour
```

### Enable HTTPS

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

### SQL Injection Prevention

✅ **DO:** Use parameterized queries
```java
@Query("SELECT t FROM Task t WHERE t.status = :status")
List<Task> findByStatus(@Param("status") String status);
```

❌ **DON'T:** String concatenation
```java
// Never do this!
String query = "SELECT * FROM tasks WHERE status = '" + status + "'";
```

---

## Database Security

### Connection Encryption

Already enabled via Oracle Wallet (mTLS).

**Verify:**
```sql
SELECT * FROM v$transport_connection_state;
-- Should show: ENCRYPTION=ACCEPTED
```

### User Access Control

```sql
-- Revoke unnecessary privileges
REVOKE ALL PRIVILEGES FROM todouser;
GRANT CONNECT, RESOURCE TO todouser;
GRANT SELECT, INSERT, UPDATE, DELETE ON TASKS TO todouser;

-- Create read-only user
CREATE USER readonly_user IDENTIFIED BY "password";
GRANT SELECT ON tasks TO readonly_user;
```

---

## Kubernetes Security

### Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: todoapp-netpol
  namespace: mtdrworkshop
spec:
  podSelector:
    matchLabels:
      app: todolistapp-springboot
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: mtdrworkshop
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 1521  # Database port
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 53   # DNS
    - protocol: UDP
      port: 53
```

**Apply:**
```bash
kubectl apply -f network-policy.yaml
```

### Pod Security Policy

```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
  - ALL
  volumes:
  - 'configMap'
  - 'emptyDir'
  - 'projected'
  - 'secret'
  - 'downwardAPI'
  - 'persistentVolumeClaim'
  hostNetwork: false
  hostIPC: false
  hostPID: false
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'MustRunAs'
  readOnlyRootFilesystem: true
```

---

## Secret Management

### Rotate Credentials

```bash
# Update database password
export NEW_PASSWORD="SecureNewPassword123"

# Update K8s secret
kubectl delete secret dbuser -n mtdrworkshop
kubectl create secret generic dbuser \
  --from-literal=db_user=TODOUSER \
  --from-literal=dbpassword=$NEW_PASSWORD \
  -n mtdrworkshop

# Force pod restart to use new credentials
kubectl delete pod -n mtdrworkshop <pod-name>
```

### Audit Secret Access

```bash
# View all secrets
kubectl get secrets -n mtdrworkshop

# Describe secret (shows when created/updated)
kubectl describe secret dbuser -n mtdrworkshop

# Never print secret values
kubectl get secret dbuser -o json | jq '.data'
```

---

## Vulnerability Scanning

### Container Image Scanning

**OCI Registry:**
```bash
oci artifacts container image-scanning-upload create \
  --repository-name todolistapp-springboot \
  --image-id <IMAGE_OCID>
```

### Dependency Scanning

```bash
# Maven
./mvnw dependency-check:check

# NPM (Frontend)
cd src/main/frontend
npm audit

# Fix vulnerabilities
npm audit fix
```

---

## Regular Security Tasks

### Daily
- Monitor application logs for errors
- Check pod status: `kubectl get pods -n mtdrworkshop`

### Weekly
- Review database audit logs
- Check for container image updates
- Run `npm audit` on frontend

### Monthly
- Rotate credentials
- Update dependencies
- Run security scanning
- Review access logs

### Quarterly
- Penetration testing
- Security audit
- Update security policies

---

**Next:** See [Troubleshooting Guide](/docs/deployment/troubleshooting).
