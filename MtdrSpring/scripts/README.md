# Scripts de flujo del proyecto

Ubicación pensada para centralizar el flujo operativo del backend y del despliegue.

Objetivo:
- `build.sh`: compilar en local y empaquetar imagen.
- `setup.sh`: preparar el entorno inicial.
- `deploy.sh`: publicar en la nube.
- `undeploy.sh`: cerrar el despliegue en la nube.

Uso:
- `source scripts/setup.sh`
- `./scripts/build.sh`
- `./scripts/deploy.sh`
- `./scripts/undeploy.sh`

Estado actual:
- Los wrappers de `scripts/` solo delegan al flujo que ya existía.
- `MtdrSpring/backend/build.sh` usa un `Dockerfile` real y simple.
- `MtdrSpring/backend/deploy.sh` todavía depende de `state_get` y de variables ya resueltas por el flujo de setup.

Qué conviene mantener aquí cuando se ordene el flujo:
- Scripts finos y explícitos, uno por tarea.
- Plantillas de variables y secrets por separado.
- Nada de valores sensibles dentro de estos scripts.

Siguiente paso recomendado:
- si se quiere correr contenedor local, hacerlo opcional y con perfil `local`.
- mantener el wallet fuera de la imagen y montarlo solo en runtime cuando haga falta.
