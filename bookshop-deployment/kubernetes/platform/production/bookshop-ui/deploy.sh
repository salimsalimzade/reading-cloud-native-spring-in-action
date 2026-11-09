#!/bin/sh

set -euo pipefail

echo "\n📦 Deploying Bookshop UI..."

kubectl apply -f resources

echo "⌛ Waiting for Bookshop UI to be deployed..."

while [ $(kubectl get pod -l app=bookshop-ui | wc -l) -eq 0 ] ; do
  sleep 5
done

echo "\n⌛ Waiting for Bookshop UI to be ready..."

kubectl wait \
  --for=condition=ready pod \
  --selector=app=bookshop-ui \
  --timeout=180s

echo "\n📦 Bookshop UI deployment completed.\n"
