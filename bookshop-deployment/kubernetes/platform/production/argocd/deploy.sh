#!/bin/sh

set -euo pipefail

echo "\n📦 Installing ArgoCD..."

# Server-side apply: Argo CD 3.x CRDs (applicationsets) are too big for the
# client-side last-applied annotation ("metadata.annotations: Too long").
kubectl apply --server-side --force-conflicts -k resources

echo "\n📦 ArgoCD installation completed.\n"
