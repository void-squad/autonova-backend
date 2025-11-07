This overlay is intended for local development and test clusters.

- `patches/image-tags.yaml` overrides deployment images to development tags.
- `patches/resources.yaml` contains optional resource limit/requests patches.

If you want to expose the gateway publicly, add an Ingress or change `gateway-service` to type LoadBalancer here (commented example could be added).
