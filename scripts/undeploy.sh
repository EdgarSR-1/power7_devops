#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[!] Undeploy en OCI requiere setup previo con flujo OCI completo."
echo "[!] Esto aún está bajo env.sh + main-destroy.sh en MtdrSpring/utils/."
echo "[!] Próximo paso: completar la migración del flujo OCI a ./scripts/."
echo ""
echo "Por ahora, si necesitas undeploy, usa:"
echo "  source MtdrSpring/env.sh"
echo "  cd MtdrSpring/backend"
echo "  ./undeploy.sh"
exit 1
