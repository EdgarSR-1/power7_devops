---
sidebar_position: 2
---

# Network Configuration

## VCN Security Lists

### Allow Ingress from Internet

**OCI Console:**
1. Networking → Virtual Cloud Networks → MtdrWorkshop
2. Security Lists → Default Security List
3. Add Ingress Rule:
   - Protocol: TCP
   - Source: 0.0.0.0/0
   - Destination Port: 80, 443

### Allow Egress to Database

**Egress Rule:**
- Protocol: TCP
- Destination: 10.0.0.0/16 (OKE subnet)
- Port: 1521 (Oracle)

---

## Load Balancer Configuration

### Static IP Reservation

```bash
# Reserve static IP
oci network public-ip create \
  --compartment-id COMPARTMENT_OCID \
  --lifetime RESERVED \
  --display-name todoapp-ip

# Associate with load balancer
oci nlb backend-set create \
  --network-load-balancer-id NLB_OCID \
  --name backend-set \
  --policy ROUND_ROBIN \
  --health-checker-protocol HTTP
```

---

## DNS Configuration

### Point Domain to Public IP

**DNS Provider (Cloudflare, Route53, etc.):**

```
Record Type: A
Name: todoapp
Value: 202.10.20.15
TTL: 3600
```

**Access via domain:**
```
http://todoapp.yourdomain.com
```

---

## SSL/TLS Certificate

### Request Certificate (OCI)

```bash
# Create certificate
oci certificates-management certificate create-by-importing-config \
  --compartment-id COMPARTMENT_OCID \
  --certificate-config type=IMPORTED_CERTIFICATE_CONFIG \
  --certificate-pem file://path/to/cert.pem \
  --private-key-pem file://path/to/key.pem
```

### Add to Load Balancer

**OCI Console:**
1. Networking → Load Balancers
2. Listeners → Add Listener
3. Protocol: HTTPS
4. Select certificate

**Backend services redirect HTTP to HTTPS:**

```bash
oci nlb listener create \
  --network-load-balancer-id NLB_OCID \
  --name https-listener \
  --protocol TLS \
  --port 443 \
  --backend-set-name backend-set \
  --ssl-configuration certificates="[\"CERTIFICATE_OCID\"]"
```

---

**Next:** See [Monitoring & Logging](/docs/guides/monitoring-logging).
