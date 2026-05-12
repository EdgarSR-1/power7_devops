K8s secrets templates
======================

Ubicación: `MtdrSpring/backend/utils/k8s-secrets/`

Propósito: contener plantillas y comandos de ejemplo para que cualquier desarrollador cree los secretos necesarios localmente antes de ejecutar `kubectl apply` sobre los manifiestos del deployment.

Reglas:
- Estos archivos SON plantillas: no deben contener valores reales ni claves.
- Para pruebas locales, ejecutar los comandos mostrados en `create-local-secrets.sh` o usar `kubectl create secret ...` directamente.

Archivos incluidos:
- `db-secrets-template.yaml` — ejemplo de Secret manifest para credenciales DB.
- `frontend-secrets-template.yaml` — ejemplo para credenciales de UI.
- `create-local-secrets.sh` — script que imprime/ejecuta los comandos `kubectl create secret` con placeholders (editar antes de ejecutar).

Nota: Documentación de uso y links están en `my-website/docs/oci/k8s-secrets.mdx`.
