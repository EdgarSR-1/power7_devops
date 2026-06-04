# JMeter Load Test Plan

This folder contains a simple pressure-test setup for the backend.

## What it does

- Runs login traffic directly against the backend load balancer.
- Executes the same request at increasing concurrency levels: 5, 10, 20, 50, 100, 200 users.
- Saves one `.jtl` file per step and a compact summary file.

## Files

- `login-load.jmx` - base JMeter plan.
- `users.csv` - sample test users.
- `register_users.sh` - helper to register users through the API.
- `run_load_steps.sh` - runner that executes the plan at each load level.

## How to run

Register users first if the backend is fixed and registration works:

```bash
cd /Users/mariolo/Dev/uni/equipo52/power7_devops
chmod +x jmeter/register_users.sh
./jmeter/register_users.sh 159.54.144.40
```

Then run the stepped load test:

```bash
chmod +x jmeter/run_load_steps.sh
./jmeter/run_load_steps.sh 159.54.144.40
```

Optional environment variables:

```bash
DURATION=180 CSV=./jmeter/users.csv ./jmeter/run_load_steps.sh 159.54.144.40
```

## Interpretation

For the report, mark a step as acceptable when:

- there are no `5xx` responses,
- `403` is gone,
- `401` only appears when you intentionally use invalid credentials,
- latency stays stable enough for the app to be usable.

Stop increasing users when response time grows sharply or the error rate starts rising.
