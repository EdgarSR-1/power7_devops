#!/usr/bin/env bash
# Usage: ./register_users.sh [HOST]
# HOST default: 140.84.181.164
set -eu
HOST=${1:-140.84.181.164}
CSV="$(dirname "$0")/users.csv"
if [ ! -f "$CSV" ]; then
  echo "users.csv not found: $CSV"
  exit 1
fi
echo "Registering users against http://$HOST/api/auth/register"
while IFS=, read -r email password name phone; do
  if [ "$email" = "email" ]; then
    continue
  fi
  payload=$(jq -n --arg name "$name" --arg email "$email" --arg phone "$phone" --arg password "$password" '{name:$name,email:$email,phone:$phone,password:$password}')
  echo -n "Register $email -> "
  http_status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://$HOST/api/auth/register" -H 'Content-Type: application/json' -d "$payload")
  echo $http_status
  sleep 0.2
done < "$CSV"

echo "Done. Check logs or try login with one of the users."
