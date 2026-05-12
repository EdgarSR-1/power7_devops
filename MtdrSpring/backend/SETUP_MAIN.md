# Backend Setup for Main Branch

## Quick Start (New Way)

From **repo root**:

```bash
./scripts/setup.sh  # One-time setup, interactive
./scripts/build.sh  # Compile Maven + Docker image
```

That's it. `.env.local` is created with your answers (profile, JWT, UI creds, Oracle wallet if needed).

## Legacy Way (Still works)

If you need manual control:

```bash
cd MtdrSpring/backend
set -a; source .env.local; set +a
mvn clean verify
./build.sh  # (Legacy script, not recommended)
```

## Required private files (not in Git)

1. `.env.local` in `MtdrSpring/backend/` — created by `./scripts/setup.sh`
2. Oracle wallet (if using Oracle):
   - Place in `MtdrSpring/backend/wallet/`
   - See template in `MtdrSpring/backend/wallet.example/README.md`

## Common failures

1. ORA-01400 on `SPRINTS.GROUP_ID`:
   - Create at least one task group before creating sprints, or send `groupId` in sprint creation.
2. Oracle connection errors:
   - Verify `db_url` contains valid `TNS_ADMIN` path and wallet files are present.
3. Bot auth/config errors:
   - Verify `TELEGRAM_BOT_TOKEN` and `TELEGRAM_BOT_NAME`.
