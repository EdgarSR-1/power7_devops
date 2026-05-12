#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
backend_dir="$repo_root/MtdrSpring/backend"
env_file="$backend_dir/.env.local"

if [[ ! -f "$env_file" ]]; then
  echo "Falta $env_file."
  echo "Este repo espera que primero corras ./scripts/setup.sh para crear la configuración local."
  echo "La setup te pedirá: perfil (local/oracle), JWT secret, credenciales UI y, si usas Oracle, datos DB y wallet."

  if [[ -t 0 ]]; then
    read -r -p "¿Quieres correr ./scripts/setup.sh ahora? [Y/n] " answer
    answer="${answer:-Y}"
    if [[ "$answer" =~ ^[Yy]$ ]]; then
      "$script_dir/setup.sh"
    else
      exit 1
    fi
  else
    exit 1
  fi
fi

cd "$backend_dir"

if [[ ! -x ./mvnw ]]; then
  if [[ -f ./mvnw ]]; then
    echo "[*] ./mvnw existe pero no es ejecutable; se lanzará con bash."
    mvnw_cmd=(bash ./mvnw)
  else
    echo "No se encontró ./mvnw en $backend_dir"
    exit 1
  fi
else
  mvnw_cmd=(./mvnw)
fi

echo "[*] Compilando backend con Maven..."
"${mvnw_cmd[@]}" -DskipTests package

echo "[*] Construyendo imagen Docker..."
image_name="agileimage:0.1"
docker build -f Dockerfile -t "$image_name" .
echo "[✓] Imagen construida: $image_name"

if [[ "${RUN_LOCAL:-0}" == "1" ]]; then
  echo "[*] Ejecutando contenedor local..."
  container_name="agilecontainer"
  docker stop "$container_name" 2>/dev/null || true
  docker rm -f "$container_name" 2>/dev/null || true

  run_args=(--name "$container_name" -p 8080:8080 --env-file "$env_file")

  if grep -q '^SPRING_PROFILES_ACTIVE=oracle$' "$env_file" && [[ -d "$backend_dir/wallet" ]]; then
    run_args+=(--volume "$backend_dir/wallet:/mtdrworkshop/creds:ro")
  fi

  docker run "${run_args[@]}" -d "$image_name"
  echo "[✓] Contenedor levantado en http://localhost:8080"
fi
