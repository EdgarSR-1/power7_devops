# Power7 DevOps Workspace

Quick start:

```bash
./scripts/setup.sh  # Crea .env.local (interactivo, una sola vez)
./scripts/build.sh  # Compila y construye imagen Docker
```

If this is a fresh clone, the backend runtime files are created locally:

1. Run `./scripts/setup.sh`
2. Answer the prompts for `JWT_SECRET`, Telegram, Oracle DB, and wallet path
3. Confirm that `MtdrSpring/backend/.env.local` exists
4. Run `./scripts/build.sh`
5. Optional local run: `RUN_LOCAL=1 ./scripts/build.sh`

`JWT_SECRET` is stored in `MtdrSpring/backend/.env.local`, not in the wallet. The wallet is only for Oracle connectivity at runtime.

From there you can deploy to OCI using the flow of your environment.

Para detalle completo: `my-website/docs/oci/` tiene la documentación de arquitectura, configuración y troubleshooting.

Todos los scripts viven en `./scripts/` desde la raíz. Sin excepciones.
