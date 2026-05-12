#!/usr/bin/env bash
set -euo pipefail

# Script de ejemplo para crear secrets locales. Editar los valores antes de ejecutar.

echo "Este script creará secretos en el cluster actual (kubeconfig). Edita los valores antes de ejecutar."

# DB secrets (editar antes de ejecutar):
DB_USER=REPLACE_ME_DB_USER
DB_PASSWORD=REPLACE_ME_DB_PASSWORD

echo "Creando secret 'dbuser'..."
kubectl create secret generic dbuser \
  --from-literal=dbuser="$DB_USER" \
  --from-literal=dbpassword="$DB_PASSWORD" || true

# Wallet: espera un directorio llamado wallet/ con los archivos necesarios
echo "Para crear 'db-wallet-secret' desde un directorio 'wallet/', usa:
  kubectl create secret generic db-wallet-secret --from-file=wallet/"

# Frontend admin
UI_USER=admin
UI_PASSWORD=REPLACE_ME_UI_PASSWORD
echo "Creando secret 'frontendadmin'..."
kubectl create secret generic frontendadmin \
  --from-literal=username="$UI_USER" \
  --from-literal=password="$UI_PASSWORD" || true

echo "Hecho. Verifica con: kubectl get secrets"
