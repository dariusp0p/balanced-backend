# balanced-backend

Spring Boot in-memory backend for food logging.

## Requirements

- Java 21+
- Maven 3.9+

## Run locally

```bash
mvn spring-boot:run
```

## Run tests

```bash
mvn test
```

## Seeded demo user

The app seeds one demo user and starter logs at startup (configurable in `src/main/resources/application.properties`).

- email: `demo@balanced.local`
- password: `password123`

## API highlights

- `GET /api/food-logs/day?date=YYYY-MM-DD`
- `POST /api/food-logs/generator/start`
- `POST /api/food-logs/generator/stop`
- `GET /api/food-logs/generator/status`

All `/api/**` endpoints require:

- `Authorization: Bearer <token>`

## Generator request body

`POST /api/food-logs/generator/start` accepts an optional body:

```json
{
  "batchSize": 5,
  "intervalMs": 3000
}
```

## WebSocket notifications

When a batch is generated, the backend publishes an event to:

- `/topic/food-logs/{userId}`

WebSocket STOMP endpoint:

- `/ws`

## Frontend offline sync strategy

Recommended approach for CRUD offline support:

1. Keep an in-memory + persisted local queue of mutations (create/update/delete).
2. Detect offline via `navigator.onLine` and fetch/network failures.
3. Apply optimistic updates locally so UI remains responsive.
4. On reconnect, replay queued mutations in order to backend.
5. After successful replay, fetch latest server state (or handle WebSocket events) to resolve drift.
6. Use a `clientMutationId` per queued operation to avoid duplicate replays.
