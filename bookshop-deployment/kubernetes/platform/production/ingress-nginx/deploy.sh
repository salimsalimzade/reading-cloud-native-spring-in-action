#!/usr/bin/env bash
set -euo pipefail

# Guide Step 4: generic "cloud" manifest instead of the book's provider/do one.
# resources/kustomization.yml points at
#   https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/cloud/deploy.yaml
# and adds patch-controller.yml (AppArmor Unconfined), without which NGINX
# can't reload on Ubuntu and the controller never becomes ready.
# If that URL returns 404, change v1.15.1 to v1.15.0 in resources/kustomization.yml.
cd "$(dirname "$0")"

echo "Deploying ingress-nginx..."
kubectl apply -k resources

echo "Waiting for ingress-nginx to be ready..."
# rollout status (instead of `kubectl wait --selector`) waits for the *new*
# controller pod, so an old, unready pod being replaced can't fail the wait.
kubectl rollout status deployment/ingress-nginx-controller \
  --namespace ingress-nginx \
  --timeout=180s
