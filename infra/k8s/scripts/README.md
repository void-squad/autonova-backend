Usage
-----

- ./apply-dev.sh — Creates the `autonova-env` secret from `secrets/.env` and applies `overlays/dev`.
- ./teardown.sh — Deletes the `autonova` namespace and all resources in it.

Before running apply-dev.sh:

```bash
cd infra/k8s
cp secrets/.env.example secrets/.env
# edit secrets/.env with real values
./scripts/apply-dev.sh
```
