# balanced-backend

Spring Boot backend for food logging with PostgreSQL persistence.

## Requirements

- Java 21+
- Maven 3.9+
- Docker (for local PostgreSQL)

## Run locally

Start PostgreSQL first:

```bash
docker compose up -d postgres
```

Then run the application:

```bash
mvn spring-boot:run
```

## Run tests

```bash
mvn test
```

Tests use an in-memory H2 database, while the application uses PostgreSQL by default.

## Seeded demo user

The app seeds one demo user and starter logs at startup (configurable in `src/main/resources/application.properties`).

- email: `demo@balanced.local`
- password: `password123`

## API highlights

- `GET /api/food-logs/day?date=YYYY-MM-DD`
- `POST /api/food-logs/generator/start`
- `POST /api/food-logs/generator/stop`
- `GET /api/food-logs/generator/status`
- `GET /api/log-groups?page=0&size=10`
- `POST /api/log-groups`
- `GET /api/log-groups/{id}`
- `PUT /api/log-groups/{id}`
- `DELETE /api/log-groups/{id}`

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

## Log group payload

`POST /api/log-groups` and `PUT /api/log-groups/{id}` use:

```json
{
  "name": "Training Day",
  "date": "2024-03-24",
  "computeFromFoodLogs": true,
  "totalCalories": 0,
  "totalProtein": 0,
  "totalCarbs": 0,
  "totalFats": 0
}
```

- If `computeFromFoodLogs` is `true`, totals are computed from that user's logs for the same date.
- If `computeFromFoodLogs` is `false`, totals are returned from the stored values.

## GraphQL reads

GraphQL endpoint:

- `POST /graphql`

All GraphQL queries require `Authorization: Bearer <token>`.

Example query:

```graphql
query {
  logGroups(page: 0, size: 10) {
    totalElements
    content {
      id
      name
      date
      totalCalories
    }
  }
}
```

Available queries:

- `logGroups(page, size)`
- `logGroup(id)`
- `foodLogs(page, size)`
- `foodLogsByDate(date)`

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
