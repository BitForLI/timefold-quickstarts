# Fulfilment Optimizer

A Java 21 service that assigns e-commerce order lines to fulfilment centres while respecting operational limits and
optimising delivery decisions. It is an original extension developed in this fork of the Timefold Solver Quickstarts.
The surrounding fork retains the upstream vehicle-routing, scheduling and other solver examples; this README describes
the fulfilment module added here, not authorship of those existing examples.

## What it solves

The Timefold model treats each order line as a planning entity and each fulfilment centre as a possible assignment.
The score separates feasibility from business trade-offs:

- Hard constraints penalise assignments that exceed a centre's daily capacity or SKU inventory or miss the order SLA.
  They prioritise feasibility in the score, but cannot guarantee a feasible answer when the input has no feasible plan.
- Soft constraints minimise lane-level delivery cost and order splitting, then discourage concentrated load by using a
  squared utilisation penalty.
- A deterministic, capacity-aware greedy allocator provides a reproducible baseline and, when it finds a feasible
  assignment, a warm start for the solver. Otherwise the solver starts with unassigned lines.

## Service design

`POST /api/fulfilment-plans` accepts a plan with an `Idempotency-Key` header and returns `202 Accepted`. The service
persists the request before starting an asynchronous Timefold job. Reusing the key with the same request returns the
original job; using it for different input returns `409 Conflict`.

`GET /api/fulfilment-plans/{jobId}` returns the persisted job state and best solution. `DELETE` requests early
termination of a running job. `POST /api/fulfilment-plans/baseline` runs the deterministic greedy allocator without
starting Timefold.

Job state and solution snapshots are stored in PostgreSQL. Job lifecycle events are inserted into an outbox table in
the same transaction as their state changes. When SQS publishing is enabled, a scheduled dispatcher sends them to SQS;
event IDs are message attributes so downstream consumers can deduplicate at-least-once delivery. Compose enables this
path with LocalStack. Without `SQS_ENABLED=true`, the publisher logs events instead of sending them to SQS.

## Run locally

Prerequisites: Docker with Compose.

```shell
docker compose up --build
```

The API is available at `http://localhost:8080`; OpenAPI is at `http://localhost:8080/q/openapi`, and health checks are
at `http://localhost:8080/q/health`. PostgreSQL and LocalStack SQS are included in the Compose stack.

Submit the included fixture:

```shell
curl -i -X POST http://localhost:8080/api/fulfilment-plans \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-001" \
  --data @src/test/resources/sample-plan.json
```

For a local JVM build:

```shell
mvn verify
mvn quarkus:dev
```

## Deployment

- `Dockerfile` creates a non-root Java 21 runtime image.
- `terraform/` defines encrypted PostgreSQL RDS plus an SQS queue and dead-letter queue. Network IDs are explicit
  inputs so the database remains in caller-managed private subnets; the definitions do not prove a live deployment.
- `.github/workflows/fulfilment-optimizer.yml` runs unit/API tests and builds the container on scoped changes.

## Test coverage

Constraint tests isolate capacity, inventory, SLA, delivery-cost and split scoring. Baseline tests verify deterministic
assignment, while Quarkus API tests cover baseline output, required idempotency keys and replay behaviour using an H2
test database. The current test suite does not prove SQS delivery, globally optimal solutions or production throughput.

## Evidence for project descriptions

| Claim | Source |
|---|---|
| Capacity, inventory, SLA, cost, split and load scoring | [`FulfilmentConstraintProvider.java`](src/main/java/dev/reese/fulfilment/solver/FulfilmentConstraintProvider.java) and its constraint tests |
| Deterministic baseline and conditional warm start | [`GreedyFulfilmentAllocator.java`](src/main/java/dev/reese/fulfilment/solver/GreedyFulfilmentAllocator.java) and [`FulfilmentPlanningService.java`](src/main/java/dev/reese/fulfilment/service/FulfilmentPlanningService.java) |
| Idempotent asynchronous API and persisted job state | [`FulfilmentPlanResource.java`](src/main/java/dev/reese/fulfilment/rest/FulfilmentPlanResource.java), [`PlanJobStore.java`](src/main/java/dev/reese/fulfilment/persistence/PlanJobStore.java) and API tests |
| Transactional outbox and conditional SQS publishing | [`PlanJobStore.java`](src/main/java/dev/reese/fulfilment/persistence/PlanJobStore.java), [`OutboxDispatcher.java`](src/main/java/dev/reese/fulfilment/messaging/OutboxDispatcher.java) and [`SqsEventPublisher.java`](src/main/java/dev/reese/fulfilment/messaging/SqsEventPublisher.java) |

These links support implementation claims, not measured speed, deployment or commercial impact.

## Attribution

This module was created by Reese Lee in a fork of
[TimefoldAI/timefold-quickstarts](https://github.com/TimefoldAI/timefold-quickstarts). It uses Timefold Solver under the
repository's Apache License 2.0. The existing `order-picking` quickstart was used as an API and modelling reference; no
upstream example was presented as original work.
