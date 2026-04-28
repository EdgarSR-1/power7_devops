#!/usr/bin/env bash

set -e  # stop on error

CONTAINER_NAME="agilecontainer"
IMAGE_NAME="agileimage:0.1"

echo "Stopping container (if running)..."
docker stop "$CONTAINER_NAME" 2>/dev/null || true

echo "Removing container (if exists)..."
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

echo "Removing image (if exists)..."
docker rmi "$IMAGE_NAME" 2>/dev/null || true

echo "Building project with Maven..."
mvn clean verify

echo "Building Docker image..."
docker build -f Dockerfile --platform linux/amd64 -t "$IMAGE_NAME" .

echo "Running container..."
docker run \
  --name "$CONTAINER_NAME" \
  --env-file .env \
  --volume "$(pwd)/target:/tmp/target:rw" \
  -p 8080:8080 \
  -d "$IMAGE_NAME"

echo "Container is running on http://localhost:8080"
