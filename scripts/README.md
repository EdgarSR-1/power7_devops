# Scripts del proyecto

Punto de entrada único y centralizado.

Uso:
- `./scripts/setup.sh` → prepara `.env.local` interactivo una sola vez.
- `./scripts/build.sh` → compila el backend y construye la imagen Docker.
- `./scripts/deploy.sh` → deploy en OCI (próximamente, por ahora en desarrollo).
- `./scripts/undeploy.sh` → undeploy en OCI (próximamente, por ahora en desarrollo).

Reglas:
- `build.sh` no esconde configuración: si falta algo, avisa qué falta.
- `setup.sh` pregunta datos: JWT, credenciales UI, perfil (local/oracle), wallet si es Oracle.
- El wallet vive en runtime, no en la imagen.
- Nunca comitees `.env.local` al repo.
