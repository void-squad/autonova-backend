# Kubernetes manifests (Kustomize) for Autonova

This folder provides a Kustomize-based Kubernetes configuration converted from the project's Docker Compose setup.

Quick start (dev):

```bash
cd infra/k8s
cp secrets/.env.example secrets/.env
# edit secrets/.env
./scripts/apply-dev.sh

# Check resources
kubectl -n autonova get pods
kubectl -n autonova get svc

# To access frontend locally:
kubectl -n autonova port-forward svc/web 8080:80
# To access gateway locally:
kubectl -n autonova port-forward svc/gateway-service 8080:8080
```

Notes:
- Secrets are created from `secrets/.env` by the `create-secrets-from-env.sh` script.
- The `overlays/dev` kustomization overrides images and resource settings appropriate for development.
- Services are ClusterIP by default; use port-forwarding or add an Ingress/LoadBalancer in overlays if needed.
