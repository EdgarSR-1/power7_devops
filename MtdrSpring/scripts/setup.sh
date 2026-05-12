#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "$script_dir/.." && pwd)"

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  echo "ERROR: use 'source scripts/setup.sh'"
  exit 1
fi

source "$project_root/env.sh"
source "$project_root/setup.sh"
