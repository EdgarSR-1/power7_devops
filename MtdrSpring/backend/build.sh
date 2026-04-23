#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

container_name="agilecontainer"
image_name="agileimage:0.1"
docker_platform="${DOCKER_PLATFORM:-}"
docker_memory="${DOCKER_MEMORY:-1g}"
docker_cpus="${DOCKER_CPUS:-2}"

build_args=()
if [[ -n "$docker_platform" ]]; then
	build_args+=(--platform "$docker_platform")
fi

docker stop "$container_name" 2>/dev/null || true
docker rm -f "$container_name" 2>/dev/null || true
docker rmi "$image_name" 2>/dev/null || true

mvn clean verify
docker build -f Dockerfile "${build_args[@]}" -t "$image_name" .

run_args=(--name "$container_name" \
	--memory "$docker_memory" \
	--cpus "$docker_cpus" \
	-p 8080:8080)

if [[ -f ".env.local" ]]; then
	run_args+=(--env-file .env.local)
fi

docker run "${run_args[@]}" -d "$image_name"
