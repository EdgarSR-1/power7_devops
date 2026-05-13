# Backend Setup for Main Branch

## 1) Required private files (not in Git)

1. Create local env file:
   - `cp .env.local.example .env.local`
2. Fill real values in `.env.local` from your team secrets.
3. Copy Oracle wallet files to:
   - `MtdrSpring/backend/wallet/`
   - See expected list in `MtdrSpring/backend/wallet.example/README.md`.

## 2) Local build

From `MtdrSpring/backend`:

1. Export env values:
   - `set -a; source .env.local; set +a`
2. Compile:
   - `sh mvnw -DskipTests compile`
3. Full build:
   - `sh mvnw clean verify`

## 3) Docker build/run

From `MtdrSpring/backend`:

1. Ensure `.env.local` exists.
2. Ensure `wallet/` exists and contains real files.
3. Run:
   - `./build.sh`

## 4) Common failures

1. ORA-01400 on `SPRINTS.GROUP_ID`:
   - Create at least one task group before creating sprints, or send `groupId` in sprint creation.
2. Oracle connection errors:
   - Verify `db_url` contains valid `TNS_ADMIN` path and wallet files are present.
3. Bot auth/config errors:
   - Verify `TELEGRAM_BOT_TOKEN` and `TELEGRAM_BOT_NAME`.
