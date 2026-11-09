#!/bin/sh

set -euo pipefail

echo "\n🗝️  Keycloak deployment started.\n"

echo "📦 Installing Keycloak..."

# Book: `echo $ random | openssl md5` hashes the literal text "$ random", so the
# "secret" is always 1b1b19599c2264fd218c. Use a real random value instead, but
# reuse the existing one on re-runs: Keycloak imports the realm only on first
# start, so a new value would no longer match the client secret in Keycloak.
clientSecret=$(kubectl get secret bookshop-keycloak-client-credentials \
  -o jsonpath='{.data.spring\.security\.oauth2\.client\.registration\.keycloak\.client-secret}' 2>/dev/null | base64 --decode || true)
if [ -z "$clientSecret" ]; then
  # A previous run may have stopped before creating the Secret (e.g. Keycloak
  # wasn't ready in time); the realm ConfigMap still holds the imported value.
  clientSecret=$(kubectl get configmap keycloak-config -n keycloak-system \
    -o jsonpath='{.data.realm-config\.json}' 2>/dev/null \
    | grep -o '"secret" *: *"[^"]*"' | head -1 | sed 's/.*: *"\(.*\)"/\1/' || true)
fi
if [ -z "$clientSecret" ]; then
  clientSecret=$(openssl rand -hex 10)
fi

kubectl apply -f resources/namespace.yml
sed "s/bookshop-keycloak-secret/$clientSecret/" resources/keycloak-config.yml | kubectl apply -f -

echo "\n📦 Configuring Helm chart..."

# Bitnami's classic chart repo index is gone; the same chart is on its OCI registry.
helm upgrade --install bookshop-keycloak oci://registry-1.docker.io/bitnamicharts/keycloak \
  --values values.yml \
  --namespace keycloak-system --version 21.4.1

echo "\n⌛ Waiting for Keycloak to be deployed..."

sleep 15

while [ $(kubectl get pod -l app.kubernetes.io/component=keycloak -n keycloak-system | wc -l) -eq 0 ] ; do
  sleep 15
done

echo "\n⌛ Waiting for Keycloak to be ready..."

kubectl wait \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=keycloak \
  --timeout=600s \
  --namespace=keycloak-system

echo "\n✅  Keycloak cluster has been successfully deployed."

echo "\n🔐 Your Keycloak Admin credentials...\n"

echo "Admin Username: user"
echo "Admin Password: $(kubectl get secret --namespace keycloak-system bookshop-keycloak -o jsonpath="{.data.admin-password}" | base64 --decode)"

echo "\n🔑 Generating Secret with Keycloak client secret."

kubectl delete secret bookshop-keycloak-client-credentials || true

kubectl create secret generic bookshop-keycloak-client-credentials \
    --from-literal=spring.security.oauth2.client.registration.keycloak.client-secret="$clientSecret"

echo "\n🍃 A 'bookshop-keycloak-client-credentials' has been created for Spring Boot applications to interact with Keycloak."

echo "\n🗝️  Keycloak deployment completed.\n"
