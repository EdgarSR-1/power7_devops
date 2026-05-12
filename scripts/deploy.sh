#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[!] Deploy en OCI requiere setup previo con flujo OCI completo."
echo "[!] Esto aún está bajo env.sh + main-setup.sh en MtdrSpring/utils/."
echo "[!] Próximo paso: completar la migración del flujo OCI a ./scripts/."
echo ""
echo "Por ahora, si necesitas deploy, usa:"
echo "  source MtdrSpring/env.sh"
echo "  cd MtdrSpring/backend"
echo "  ./deploy.sh"
exit 1
