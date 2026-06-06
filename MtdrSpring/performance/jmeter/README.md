# JMeter load test

This folder contains the load test for the Power7 reto backend.

## What it tests

The plan creates one test user per virtual user and then simulates this API flow:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `GET /api/tasks`
- `GET /api/group-members/me`
- `GET /api/kpis/velocity`
- `GET /api/kpis/status-distribution`
- `GET /api/kpis/completed-by-sprint`
- `GET /api/kpis/overdue-tasks`

The generated test data is scoped by a `run_id`, so each run creates a fresh batch of users.

## Run locally

Start the backend with the local H2 profile:

```bash
cd MtdrSpring/backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Then run the capacity test:

```bash
cd MtdrSpring/performance/jmeter
./run-capacity-test.sh
```

By default it tests `5 10 25 50` concurrent users for `120` seconds each.

## Run against another target

```bash
TARGET_URL=https://your-backend.example.com \
STAGES="10 25 50 75 100" \
DURATION=180 \
RAMP_UP=45 \
P95_LIMIT_MS=1500 \
ERROR_LIMIT_PCT=1 \
./run-capacity-test.sh
```

The recommended capacity is the largest stage that passes both criteria:

- p95 latency is less than or equal to `P95_LIMIT_MS`
- error rate is less than or equal to `ERROR_LIMIT_PCT`

Results are written to `results/capacity-summary-*.csv` and detailed JMeter `.jtl` files.

## Direct JMeter command

```bash
jmeter -n \
  -t reto-load-test.jmx \
  -l results/manual-run.jtl \
  -Jhost=localhost \
  -Jport=8080 \
  -Jusers=10 \
  -Jseed_users=10 \
  -Jduration=120 \
  -Jramp_up=30 \
  -Jrun_id=manual-$(date +%Y%m%d%H%M%S)
```
