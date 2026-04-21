#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

container_name="agilecontainer"
image_name="agileimage:0.1"
dockerfile="DockerfileDev"
maven_cmd="./mvnw"
platform="linux/amd64"

if [[ ! -x "$maven_cmd" ]]; then
  maven_cmd="mvn"
fi

docker stop "$container_name" 2>/dev/null || true
docker rm -f "$container_name" 2>/dev/null || true
docker rmi "$image_name" 2>/dev/null || true

"$maven_cmd" clean verify
docker build -f "$dockerfile" --platform "$platform" -t "$image_name" .
docker run --name "$container_name" --platform "$platform" --volume "${PWD}/target:/tmp/target:rw" -p 8080:8080 -d "$image_name"
