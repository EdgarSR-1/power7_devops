#!/usr/bin/env bash

set -euo pipefail

container_name="agilecontainer"
image_name="agileimage:0.1"

docker stop "$container_name" 2>/dev/null || true
docker rm -f "$container_name" 2>/dev/null || true
docker rmi "$image_name" 2>/dev/null || true

mvn clean verify
docker build -f Dockerfile --platform linux/amd64 -t "$image_name" .
docker run --name "$container_name" --volume "${PWD}/target:/tmp/target:rw" -p 8080:8080 -d "$image_name"