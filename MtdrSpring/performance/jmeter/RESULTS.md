# Load test results

Run date: 2026-05-28

Environment:

- Target: `http://localhost:8080`
- Backend profile: `local`
- Database: in-memory H2
- Bot disabled for local load testing
- Criteria: p95 latency <= 1500 ms and error rate <= 1%
- Workload: auth register, auth login, current user, tasks, group membership, and KPI reads

## Result

Recommended tested capacity for the local reto app: **300 concurrent users**.

This is the largest tested stage that passed the criteria. It is not a production ceiling; repeat the same plan against the Oracle/deployed environment before using it as a deployment SLA.

## Capacity summaries

| Users | Samples | Avg ms | p95 ms | Error % | Throughput/s | Status |
| ---: | ---: | ---: | ---: | ---: | ---: | :--- |
| 5 | 200 | 10.76 | 30 | 0.00 | 3.82 | PASS |
| 10 | 400 | 7.89 | 19 | 0.00 | 7.41 | PASS |
| 25 | 1016 | 6.98 | 12 | 0.00 | 18.60 | PASS |
| 50 | 2043 | 6.15 | 9 | 0.00 | 37.24 | PASS |
| 75 | 3024 | 5.87 | 7 | 0.00 | 55.09 | PASS |
| 100 | 4056 | 5.61 | 9 | 0.00 | 73.82 | PASS |
| 150 | 5689 | 5.53 | 69 | 0.00 | 103.37 | PASS |
| 200 | 7547 | 5.38 | 69 | 0.00 | 137.17 | PASS |
| 300 | 11303 | 5.30 | 69 | 0.00 | 205.79 | PASS |

## Test data

The JMeter plan creates one test user per virtual user per stage using a unique `run_id`.

For the highest passing stage, the load test used:

- 300 concurrent virtual users
- 300 generated test users for that stage
- Read-heavy API data for tasks, group memberships and KPIs

The current plan does not create task/group business records because task creation depends on group membership and superadmin setup. For a production-like data-volume test, seed representative groups, memberships, sprints and tasks first, then run the same capacity script against that data set.
