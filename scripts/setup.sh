#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
backend_dir="$repo_root/MtdrSpring/backend"
env_file="$backend_dir/.env.local"

echo ""
echo "════════════════════════════════════════════════════════"
echo "  Backend Setup: Configuración Local"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Este script crea .env.local con tu configuración."
echo "Documentación: ./my-website/docs/oci/secrets-guide.mdx"
echo ""

ask_value() {
  local prompt_text="$1"
  local default_value="${2:-}"
  local response=""

  if [[ -n "$default_value" ]]; then
    read -r -p "  $prompt_text [$default_value]: " response
    printf '%s' "${response:-$default_value}"
  else
    read -r -p "  $prompt_text: " response
    printf '%s' "$response"
  fi
}

ask_secret() {
  local prompt_text="$1"
  local min_length="${2:-0}"
  local response=""
  local attempt=0
  
  while true; do
    read -r -s -p "  $prompt_text: " response
    echo
    
    if [[ ${#response} -lt $min_length ]]; then
      attempt=$((attempt + 1))
      if [[ $attempt -ge 3 ]]; then
        echo "  ❌ Demasiados intentos. Saliendo."
        exit 1
      fi
      echo "  ⚠️  Mínimo $min_length caracteres. Intenta de nuevo."
    else
      break
    fi
  done
  
  printf '%s' "$response"
}

mkdir -p "$backend_dir"

# Oracle es obligatorio (no hay opción local)
echo ""
echo "PASO 1: Base de Datos"
echo "─────────────────────"
echo "  • Este proyecto SIEMPRE usa Oracle ATP en OCI"
echo "  • No hay opción local (H2)"
echo ""
profile="oracle"
echo "  ✓ Perfil: oracle"
echo ""

# PASO 2: JWT Secret
echo "PASO 2: JWT Secret"
echo "──────────────────"
echo "  • Para firmar tokens de autenticación"
echo "  • Mínimo 32 caracteres"
echo "  • Genera uno: openssl rand -base64 32"
echo ""
jwt_secret="$(ask_secret "JWT_SECRET" 32)"
echo ""

# PASO 3: Telegram (opcional)
echo "PASO 3: Telegram Bot (opcional)"
echo "───────────────────────────────"
echo "  • El bot es opcional: puedes dejarlo deshabilitado en local"
echo "  • Para usarlo, crea uno con @BotFather y proporciona el token"
echo ""
telegram_enabled="$(ask_value "TELEGRAM_ENABLED (true/false)" "false")"
telegram_auto_init="$(ask_value "TELEGRAM_BOT_AUTO_INIT (true/false)" "false")"
telegram_bot_token="$(ask_value "TELEGRAM_BOT_TOKEN (dejar vacío para desactivar)")"
echo ""
echo "  Nombre de usuario del bot (ej: my_todolist_bot)"
telegram_bot_name="$(ask_value "TELEGRAM_BOT_NAME (opcional)")"
echo ""

# PASO 4: Opcionales
echo "PASO 4: Opcionales"
echo "──────────────────"
echo "  • DeepSeek API: solo si usas IA"
echo "  • Dejar vacío si no los usas"
echo ""
deepseek_api_key="$(ask_value "DEEPSEEK_API_KEY (opcional)" "")"
deepseek_api_url="$(ask_value "DEEPSEEK_API_URL (opcional)" "https://api.deepseek.com/v1/chat/completions")"
echo ""

# Variables para Oracle
echo "PASO 5: Oracle Database (ATP en OCI)"
echo "────────────────────────────────────"
echo "  • Necesitas datos de OCI Console"
echo "  • Revisa: secrets-guide.mdx en Docusaurus"
echo ""

echo "  5a. Usuario BD (típicamente: ADMIN)"
db_user="$(ask_value "SPRING_DATASOURCE_USERNAME" "ADMIN")"
echo ""

echo "  5b. Contraseña BD"
echo "      • Mínimo 12 caracteres"
echo "      • La que pusiste al crear la instancia ATP en OCI"
dbpassword="$(ask_secret "SPRING_DATASOURCE_PASSWORD" 12)"
echo ""

echo "  5c. JDBC Connection String"
echo "      Ejemplo: jdbc:oracle:thin:@reacttodok7toc_tp"
echo "      • Usa un TNS alias y NO añadas ?TNS_ADMIN en la URL"
echo "      • TNS_ADMIN por defecto es /mtdrworkshop/creds en Docker"
db_url_raw="$(ask_value "SPRING_DATASOURCE_URL (ej: jdbc:oracle:thin:@TNS_ALIAS)")"

# Strip any ?TNS_ADMIN=... from user input if present
db_url="$db_url_raw"
if [[ "$db_url" == *"?TNS_ADMIN="* ]]; then
  db_url="${db_url%%\?TNS_ADMIN=*}"
fi

# TNS_ADMIN path inside container (default)
tns_admin="$(ask_value "TNS_ADMIN (path inside container)" "/mtdrworkshop/creds")"
echo ""

# PASO 6: Wallet
echo "PASO 6: Oracle Wallet"
echo "────────────────────"
echo "  • Descarga desde OCI > Database > tu ATP > Database Connection > Download Wallet"
echo "  • Archivo: Wallet_<nombre_db>.zip o carpeta descomprimida"
echo "  • Si ya lo copiaste, presiona Enter"
echo ""
wallet_source="$(ask_value "Ruta del Wallet ZIP o carpeta (o Enter para saltear)" "")"
echo ""

if [[ -n "$wallet_source" ]]; then
  # Expandir ~ si es necesario
  wallet_source="${wallet_source/#\~/$HOME}"
  
  if [[ -f "$wallet_source" ]]; then
    echo "  ✓ Descomprimiendo wallet..."
    mkdir -p "$backend_dir/wallet"
    rm -rf "$backend_dir/wallet"/*
    unzip -q "$wallet_source" -d "$backend_dir/wallet" 2>/dev/null || true
    echo "  ✓ Wallet copiado a: MtdrSpring/backend/wallet/"
    echo ""
  elif [[ -d "$wallet_source" ]]; then
    echo "  ✓ Copiando archivos del wallet..."
    mkdir -p "$backend_dir/wallet"
    rm -rf "$backend_dir/wallet"/*
    cp -R "$wallet_source"/* "$backend_dir/wallet/" 2>/dev/null || true
    echo "  ✓ Wallet copiado a: MtdrSpring/backend/wallet/"
    echo ""
  else
    echo "  ⚠️  No encontrado: $wallet_source"
    echo "     Cópialo manualmente después si lo necesitas."
    echo ""
  fi
# Generar .env.local (siempre Oracle)
cat > "$env_file" <<EOF
# Generado por ./scripts/setup.sh
# NO commitear esto al repo
SPRING_PROFILES_ACTIVE=oracle
JWT_SECRET=$jwt_secret

# Telegram (opcional)
TELEGRAM_ENABLED=$telegram_enabled
TELEGRAM_BOT_AUTO_INIT=$telegram_auto_init
TELEGRAM_BOT_TOKEN=$telegram_bot_token
TELEGRAM_BOT_NAME=$telegram_bot_name

# Deepseek (opcional)
DEEPSEEK_API_KEY=$deepseek_api_key
DEEPSEEK_API_URL=$deepseek_api_url

# Oracle Configuration
SPRING_DATASOURCE_URL=$db_url
SPRING_DATASOURCE_USERNAME=$db_user
SPRING_DATASOURCE_PASSWORD=$dbpassword
SPRING_DATASOURCE_DRIVER_CLASS_NAME=oracle.jdbc.OracleDriver
TNS_ADMIN=$tns_admin

# UI credentials (optional)
UI_USERNAME=$(ask_value "UI_USERNAME" "admin")
UI_PASSWORD=$(ask_value "UI_PASSWORD" "change_me")

# Backwards-compat mapping
db_user=
dbpassword=
db_url=
driver_class_name=

EOF

# Append backwards-compat mapping with current SPRING_* values
cat >> "$env_file" <<EOF
db_user=
dbpassword=
db_url=
driver_class_name=
EOF

# Replace placeholders for backwards-compat mapping explicitly
sed -i.bak -e "s|db_user=|db_user=\\${SPRING_DATASOURCE_USERNAME}|" -e "s|dbpassword=|dbpassword=\\${SPRING_DATASOURCE_PASSWORD}|" -e "s|db_url=|db_url=\\${SPRING_DATASOURCE_URL}|" -e "s|driver_class_name=|driver_class_name=\\${SPRING_DATASOURCE_DRIVER_CLASS_NAME}|" "$env_file" || true
rm -f "$env_file.bak" || true

echo "════════════════════════════════════════════════════════"
echo "  ✓ Configuración lista"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Archivo creado: $env_file"
echo ""
echo "✓ Perfil: oracle (siempre)"
echo "✓ JWT_SECRET: Configurado (32+ chars)"
echo "✓ Telegram Bot: $telegram_bot_name"
if [[ -d "$backend_dir/wallet" ]] && [[ -n "$(ls -A "$backend_dir/wallet" 2>/dev/null)" ]]; then
  echo "✓ Wallet Oracle: Sí (en MtdrSpring/backend/wallet/)"
  wallet_files=$(ls "$backend_dir/wallet" | wc -l)
  echo "  └─ Archivos: $wallet_files"
else
  echo "⚠️  Wallet Oracle: NO ENCONTRADO"
  echo "  └─ Si lo necesitas, cópialo manualmente a MtdrSpring/backend/wallet/"
fi
echo ""
echo "Próximo paso:"
echo "  ./scripts/build.sh"
echo ""
