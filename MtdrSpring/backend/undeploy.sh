#!/bin/bash

echo "Deleting backend resources..."
for ns in mtdrworkshop default; do
	kubectl -n "$ns" delete deployment todolistapp-springboot-deployment --ignore-not-found=true
	kubectl -n "$ns" delete service todolistapp-springboot-service --ignore-not-found=true
	kubectl -n "$ns" delete service todolistapp-backend-router --ignore-not-found=true
done

echo "Deleting frontend resources..."
for ns in p7-frontend default; do
	kubectl -n "$ns" delete deployment p7frontend-deployment --ignore-not-found=true
	kubectl -n "$ns" delete service p7frontend-service --ignore-not-found=true
	kubectl -n "$ns" delete service p7frontend-router --ignore-not-found=true
done

echo "Undeploy finished."
