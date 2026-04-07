# Telco Incident Management

Backend simulating a NOC (Network Operations Center) system for handling network incidents in a telco environment.

The project covers the core operational domains used in the demo:
- incidents
- network inventory via `network_node`
- maintenance planning via `maintenance_window`
- monitoring integrations via `alarm_event`

## Project Scope

The currently implemented and tested incident backend includes:
- incident creation
- detail retrieval
- listing with filtering, sorting, and pagination
- `network_node` create, detail lookup, filtered lookup, and partial update
- partial incident updates, including replacing the root node and related node list
- incident lifecycle handling
- event timeline
- maintenance window create, detail lookup, filtered paginated listing, and partial update
- alarm event create, detail lookup, filtered paginated listing, and partial update
- OpenAPI / Swagger UI
- stable pagination response DTO
- global exception handling
- closed persistence boundary with `spring.jpa.open-in-view=false`, so controllers operate on DTOs instead of lazy-loading JPA state during response rendering

Supported endpoints:
- `POST /api/incidents`
- `GET /api/incidents`
- `GET /api/incidents/{id}`
- `PATCH /api/incidents/{id}`
- `PATCH /api/incidents/{id}/acknowledge`
- `PATCH /api/incidents/{id}/resolve`
- `PATCH /api/incidents/{id}/close`
- `GET /api/incidents/{id}/timeline`
- `POST /api/network-nodes`
- `GET /api/network-nodes`
- `GET /api/network-nodes/{id}`
- `PATCH /api/network-nodes/{id}`
- `POST /api/maintenance-windows`
- `GET /api/maintenance-windows`
- `GET /api/maintenance-windows/{id}`
- `PATCH /api/maintenance-windows/{id}`
- `POST /api/alarm-events`
- `GET /api/alarm-events`
- `GET /api/alarm-events/{id}`
- `PATCH /api/alarm-events/{id}`

## Stack

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA / Hibernate
- Bean Validation
- PostgreSQL
- Flyway
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Testcontainers
- Elasticsearch + Logstash + Kibana (optional, local)

## Repository Architecture

Main application code:
- `src/main/java/pl/telco/incident/controller` - REST API
- `src/main/java/pl/telco/incident/service` - business logic
- `src/main/java/pl/telco/incident/repository` - JPA repositories and specifications
- `src/main/java/pl/telco/incident/entity` - domain entities
- `src/main/java/pl/telco/incident/dto` - API requests and responses
- `src/main/java/pl/telco/incident/exception` - API errors and exception handler
- `src/main/java/pl/telco/incident/observability` - shared event logger, audit listener, and application lifecycle logs
- `src/main/java/pl/telco/incident/config` - Swagger and data seeding

Database:
- `src/main/resources/db/migration/V1__init.sql`
- `src/main/resources/db/migration/V2__add_closed_at_to_incident.sql`
- `src/main/resources/db/migration/V3__add_incident_version_and_query_indexes.sql`

Tests:
- `src/test/java` - unit, WebMvc, integration, and OpenAPI tests
- `src/test/resources/application-test.yaml` - test configuration

## Incident Domain Model

An incident represents an operational problem in the telco network. At the API and model level it currently includes:
- technical identifier `id`
- operator-facing number `incidentNumber`
- `title`
- `priority`
- `status`
- `region`
- `sourceAlarmType`
- `possiblyPlanned`
- `rootNodeId`
- list of related nodes
- lifecycle timestamps:
  - `openedAt`
  - `acknowledgedAt`
  - `resolvedAt`
  - `closedAt`

The related timeline stores business events such as:
- `CREATED`
- `UPDATED`
- `ACKNOWLEDGED`
- `RESOLVED`
- `CLOSED`

## Lifecycle

Allowed status transitions:
- `OPEN -> ACKNOWLEDGED`
- `ACKNOWLEDGED -> RESOLVED`
- `RESOLVED -> CLOSED`

Each lifecycle action:
- updates the status
- sets the corresponding timestamp
- optionally stores a note
- appends a timeline entry

## Local Run

### Requirements

- Java 21
- Docker Desktop or local PostgreSQL
- Maven Wrapper from the repository

### Full Stack in Docker Compose

If you want to run the full backend stack in containers:

```powershell
docker compose up -d --build
```

Important:
- do not run `docker-compose.yml` and `docker-compose.elk.yml` at the same time
- `docker-compose.yml` already includes `elasticsearch`, `logstash`, and `kibana`
- if you started a separate ELK stack earlier, stop it first:

```powershell
docker compose -f docker-compose.elk.yml down
```

This compose starts:
- `postgres`
- `backend`
- `elasticsearch`
- `logstash`
- `kibana`

Available after startup:
- backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

The backend container:
- connects to `postgres` over the Compose network
- runs with profiles `dev,elk`
- sends logs to `logstash`
- runs Flyway on startup

If local ports are already in use, you can override mappings without editing the file:

```powershell
$env:POSTGRES_PORT=55432
docker compose up -d --build
```

You can override these as well:
- `BACKEND_PORT`
- `POSTGRES_PORT`
- `ELASTICSEARCH_PORT`
- `KIBANA_PORT`
- `LOGSTASH_PORT`
- `LOGSTASH_MONITORING_PORT`

Stop the stack:

```powershell
docker compose down
```

If you also want to remove data volumes:

```powershell
docker compose down -v
```

### Database

Default development configuration is in `src/main/resources/application.yaml`:

- URL: `jdbc:postgresql://localhost:5432/telco_incident_db`
- user: `postgres`
- application port: `8080`
- set the database password with `SPRING_DATASOURCE_PASSWORD`

Minimal local PostgreSQL setup:

```sql
CREATE DATABASE telco_incident_db;
```

Flyway runs migrations automatically on startup.

If you do not want to use the default configuration, change `application.yaml` or override properties when starting the application.

### Start the Application

Development mode:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Build and run the jar:

```powershell
.\mvnw clean package
java -jar target/telco-incident-management-0.0.1-SNAPSHOT.jar
```

### Swagger / OpenAPI

After starting the application:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Data Seeding

The seeder is disabled by default outside the `dev` profile and runs only when tables are empty.

Configuration:
- property: `app.seed.enabled`
- default: `false`
- `dev` profile: `true`
- tests: `false`

The seeder adds sample:
- network nodes
- incidents
- maintenance windows
- alarm events
- incident timeline entries

Implementation: `src/main/java/pl/telco/incident/config/DataInitializer.java`

Example of disabling the seeder:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-Dapp.seed.enabled=false"
```

## Observability and ELK

The application includes the basics for centralized logging:
- `X-Request-Id` for every request
- HTTP-layer request/response logging
- business logs for incident create, update, and lifecycle actions
- error logging in `GlobalExceptionHandler`
- JPA entity audit logging for `incident`, `incident_node`, `incident_timeline`, and `network_node`
- persistence logs for `maintenance_window`, `maintenance_node`, and `alarm_event`
- `elk` profile that sends JSON logs directly to Logstash over TCP
- Spring Boot Actuator for free observability endpoints
- business lifecycle and incident change metrics via Micrometer

### Actuator

Available endpoints:
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/info`
- `GET /actuator/metrics`

This is intentionally a lightweight, free observability package suitable for a student project:
- health checks for local run and Docker
- basic application metadata
- runtime metrics without using paid platform features
- business counters and timers for incident lifecycle

### Running a Local ELK Stack

If you want to run only ELK without the backend and without PostgreSQL, you can still use the lightweight compose:

```powershell
docker compose -f docker-compose.elk.yml up -d
```

This variant is an alternative to the full `docker compose up -d --build`, not an addition to it.

Start containers:

```powershell
docker compose -f docker-compose.elk.yml up -d
```

Available endpoints:
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- Logstash TCP input: `localhost:5000`

### Starting the Application with ELK Profile

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev,elk"
```

After startup, logs go to Logstash and then to Elasticsearch index:

```text
telco-incident-management-YYYY.MM.dd
```

In Kibana it is worth creating a data view:

```text
telco-incident-management-*
```

Example log fields:
- `requestId`
- `eventDataset`
- `eventCategory`
- `eventAction`
- `entityType`
- `entityId`
- `tableName`
- `method`
- `path`
- `status`
- `durationMs`
- `timelineEventType`
- `incidentId`
- `incidentNumber`
- `incidentStatus`
- `priority`
- `region`
- `possiblyPlanned`
- `service`

Mainly used datasets:
- `http`
- `system`
- `incident`
- `network_node`
- `maintenance`
- `alarm`

Example business metrics:
- `incident.created`
- `incident.updated`
- `incident.lifecycle.transition`
- `incident.time.to_ack`
- `incident.time.to_resolve`
- `incident.time.to_close`

### Kibana Starter Pack

The repository includes a simple Kibana starter pack:
- importable dashboard: `elk/kibana/saved-objects/telco-incident-observability.ndjson`
- import script: `elk/kibana/import-saved-objects.ps1`
- demo API traffic generator: `elk/demo/generate-dashboard-data.ps1`
- ready-to-use KQL pack: `elk/kibana/queries/incident-kql.md`

Import the dashboard:

```powershell
.\elk\kibana\import-saved-objects.ps1
```

### Quickly Feeding the Dashboard with Data

If you want to quickly see non-empty Kibana panels, run the traffic generator:

```powershell
.\elk\demo\generate-dashboard-data.ps1
```

By default the script:
- assumes a local backend at `http://localhost:8080`
- uses seeded nodes `1` as `ROOT` and `2,3` as `AFFECTED`
- creates several incidents
- performs business-field updates and replaces incident node assignments
- creates and updates maintenance windows
- creates, updates, and correlates alarm events
- adds several `GET` calls for list, detail, timeline, and filtered paginated searches

If you have different node IDs or a different application URL:

```powershell
.\elk\demo\generate-dashboard-data.ps1 -BaseUrl http://localhost:8080 -RootNodeId 1 -AffectedNodeIds 2,3 -IncidentCount 4
```

After running the script, set the Kibana time range to `Last 24 hours` and click `Refresh`.

### Quick Demo Scenario

The simplest demo flow:

Script that starts the whole demo:

```powershell
.\elk\demo\start-demo.ps1
```

In Kibana click `Analytics`, `Dashboard`, then `Telco Platform Observability`.

After the demo, stop it with:

```powershell
docker compose down -v
```

Alternatively, run everything manually:

1. Start the full stack:

```powershell
docker compose up -d --build
```

2. Import the dashboard into Kibana:

```powershell
.\elk\kibana\import-saved-objects.ps1
```

3. Generate API traffic:

```powershell
.\elk\demo\generate-dashboard-data.ps1
```

4. Open:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Kibana: `http://localhost:5601`

5. In Kibana open the `Telco Platform Observability` dashboard, set `Last 24 hours`, and click `Refresh`.

After import, this dashboard will appear in Kibana:

```text
Telco Platform Observability
```

After the demo, stop it with:

```powershell
docker compose down -v
```

This is not a large production dashboard. It is a lightweight starter for a student project:
- metric with the total number of platform events
- metric with HTTP errors
- distribution by `eventDataset`
- persistence changes grouped by `tableName`
- events over time
- activity for incident, alarms, maintenance, and network lookups
- recent event tables for quick drilldown

## Incident API

### Create Incident

`POST /api/incidents`

Example request:

```json
{
  "incidentNumber": "INC-100",
  "title": "Router failure in Warsaw",
  "priority": "HIGH",
  "region": "MAZOWIECKIE",
  "sourceAlarmType": "HARDWARE",
  "possiblyPlanned": false,
  "rootNodeId": 1,
  "nodes": [
    {
      "networkNodeId": 1,
      "role": "ROOT"
    },
    {
      "networkNodeId": 2,
      "role": "AFFECTED"
    }
  ]
}
```

Business rules:
- `incidentNumber` must be unique
- `nodes` cannot contain duplicate `networkNodeId`
- exactly one node must have role `ROOT`
- `rootNodeId` must point to the same node as the record with role `ROOT`

### Get Incident

`GET /api/incidents/{id}`

Returns incident details:
- current status and lifecycle timestamps
- `rootNodeId`
- `sourceAlarmType`
- `possiblyPlanned`
- list of related nodes with roles and basic inventory data

### Update Incident

`PATCH /api/incidents/{id}`

The endpoint supports partial updates of:
- `incidentNumber`
- `title`
- `priority`
- `region`
- `sourceAlarmType`
- `possiblyPlanned`
- `rootNodeId`
- `nodes`

Rules:
- a `CLOSED` incident cannot be edited
- an empty or no-op patch returns `400`
- changing `incidentNumber` enforces uniqueness
- `rootNodeId` can only be sent together with `nodes`
- `nodes` cannot contain duplicate `networkNodeId`
- exactly one node in `nodes` must have role `ROOT`
- when `rootNodeId` is provided, it must point to the same node that is marked as `ROOT`
- a successful update appends an `UPDATED` event to the timeline

Example request:

```json
{
  "title": "Updated incident title",
  "priority": "CRITICAL",
  "sourceAlarmType": "POWER",
  "rootNodeId": 3,
  "nodes": [
    {
      "networkNodeId": 3,
      "role": "ROOT"
    },
    {
      "networkNodeId": 1,
      "role": "AFFECTED"
    }
  ]
}
```

### Lifecycle Endpoints

- `PATCH /api/incidents/{id}/acknowledge`
- `PATCH /api/incidents/{id}/resolve`
- `PATCH /api/incidents/{id}/close`

The body may contain an optional note:

```json
{
  "note": "Traffic rerouted"
}
```

### Timeline

`GET /api/incidents/{id}/timeline`

Returns events ordered ascending by `createdAt`.

### Network Node Lookup

`GET /api/network-nodes`

Helper endpoint for frontend and create/update forms.

Supported filters:
- `q` - case-insensitive partial match on `nodeName`
- `region`
- `nodeType`
- `active`

### Get Network Node

`GET /api/network-nodes/{id}`

Returns one inventory node with its current technical metadata.

## Maintenance Window API

- `POST /api/maintenance-windows`
- `GET /api/maintenance-windows`
- `GET /api/maintenance-windows/{id}`
- `PATCH /api/maintenance-windows/{id}`

Maintenance windows can be created with linked `nodeIds`, retrieved by ID, listed with filters and pagination, and later partially updated.

Editable fields:
- `title`
- `description`
- `status`
- `startTime`
- `endTime`
- `nodeIds`

Rules:
- an empty or no-op patch returns `400`
- `endTime` must be later than `startTime`
- `nodeIds`, when provided, replace the current maintenance-node links
- all referenced nodes must exist

Listing supports:
- pagination with `page` and `size`
- sorting with `sortBy` and `direction`
- filters: `status`, `statuses`, `title`, `nodeId`, `startFrom`, `startTo`, `endFrom`, `endTo`

The list returns the same stable pagination shape used by incidents:

```json
{
  "content": [],
  "number": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Example patch request:

```json
{
  "title": "Core maintenance updated",
  "status": "IN_PROGRESS",
  "startTime": "2026-03-31T21:00:00",
  "endTime": "2026-03-31T23:30:00",
  "nodeIds": [1, 3]
}
```

## Alarm Event API

- `POST /api/alarm-events`
- `GET /api/alarm-events`
- `GET /api/alarm-events/{id}`
- `PATCH /api/alarm-events/{id}`

Alarm events can be created for a `networkNodeId`, optionally correlated to an `incidentId`, retrieved by ID, filtered with pagination, and partially updated.

Editable fields:
- `incidentId`
- `alarmType`
- `severity`
- `status`
- `description`
- `suppressedByMaintenance`
- `occurredAt`

Listing supports:
- pagination with `page` and `size`
- sorting with `sortBy` and `direction`
- filters: `severity`, `severities`, `status`, `statuses`, `sourceSystem`, `externalId`, `alarmType`, `networkNodeId`, `incidentId`, `suppressedByMaintenance`, `occurredFrom`, `occurredTo`, `receivedFrom`, `receivedTo`

## Incident Listing

`GET /api/incidents`

The list returns a stable DTO instead of raw `PageImpl`:

```json
{
  "content": [],
  "number": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

The `content` list contains incident summary data. Details with nodes and extra fields are returned by `GET /api/incidents/{id}` and mutating responses.

### Pagination

- `page` - zero-based page index
- `size` - from `1` to `100`

### Sorting

Parameters:
- `sortBy`
- `direction`

Supported sort fields:
- `openedAt`
- `acknowledgedAt`
- `resolvedAt`
- `closedAt`
- `incidentNumber`
- `priority`
- `title`

For `acknowledgedAt`, `resolvedAt`, and `closedAt`, sorting supports `NULLS_LAST`.

### Filters

Single-value filters:
- `priority`
- `region`
- `possiblyPlanned`
- `status`
- `incidentNumber`
- `title`
- `sourceAlarmType`
- `openedFrom`
- `openedTo`
- `acknowledgedFrom`
- `acknowledgedTo`
- `resolvedFrom`
- `resolvedTo`
- `closedFrom`
- `closedTo`

Multi-value filters:
- `priorities`
- `statuses`

`priorities` and `statuses` support:
- repeated parameters
- comma-separated values

Date ranges use ISO-8601 format, for example `2026-03-29T10:15:00`.

`region` and `sourceAlarmType` are domain types and accept enum values such as `MAZOWIECKIE`, `HARDWARE`.

Example requests:

```text
GET /api/incidents?status=OPEN
GET /api/incidents?statuses=OPEN,ACKNOWLEDGED
GET /api/incidents?priorities=HIGH,CRITICAL&sortBy=closedAt&direction=desc
GET /api/incidents?incidentNumber=INC-10&title=router
GET /api/incidents?openedFrom=2026-03-29T08:00:00&openedTo=2026-03-29T12:00:00
GET /api/incidents?closedFrom=2026-03-29T10:00:00&closedTo=2026-03-29T12:00:00
```

### List Validation

The API returns `400` when:
- `sortBy` is unsupported
- `direction` is unsupported
- enums in filters are invalid
- any date range has `from > to`
- `page` or `size` are outside the allowed range

## Example curl Calls

Create:

```bash
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "incidentNumber": "INC-201",
    "title": "Power issue in Krakow",
    "priority": "HIGH",
    "region": "MALOPOLSKIE",
    "sourceAlarmType": "POWER",
    "possiblyPlanned": false,
    "rootNodeId": 1,
    "nodes": [
      { "networkNodeId": 1, "role": "ROOT" },
      { "networkNodeId": 2, "role": "AFFECTED" }
    ]
  }'
```

Partial update:

```bash
curl -X PATCH http://localhost:8080/api/incidents/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Power issue in Krakow - escalated",
    "priority": "CRITICAL"
  }'
```

Lifecycle:

```bash
curl -X PATCH http://localhost:8080/api/incidents/1/acknowledge \
  -H "Content-Type: application/json" \
  -d '{ "note": "NOC accepted incident" }'
```

Listing:

```bash
curl "http://localhost:8080/api/incidents?page=0&size=20&statuses=OPEN,ACKNOWLEDGED&priorities=HIGH,CRITICAL&region=MAZOWIECKIE&sortBy=openedAt&direction=desc"
```

Maintenance listing:

```bash
curl "http://localhost:8080/api/maintenance-windows?page=0&size=10&statuses=PLANNED,IN_PROGRESS&nodeId=2&sortBy=startTime&direction=desc"
```

Alarm update:

```bash
curl -X PATCH http://localhost:8080/api/alarm-events/1 \
  -H "Content-Type: application/json" \
  -d '{
    "incidentId": 1,
    "status": "ACKNOWLEDGED",
    "suppressedByMaintenance": true
  }'
```

## Error Handling

The API has global exception handling and returns predictable responses for the most common error classes:
- `400 Bad Request` - request validation, invalid query params, invalid lifecycle transition, empty patch
- `404 Not Found` - missing incident or missing node reference
- `409 Conflict` - business conflict, for example duplicate `incidentNumber`

Key file: `src/main/java/pl/telco/incident/exception/GlobalExceptionHandler.java`

## Tests

Run all tests:

```powershell
.\mvnw test
```

Test scope:
- incident service unit tests
- controller and exception handling WebMvc tests
- API integration tests on PostgreSQL via Testcontainers
- network node, maintenance window, and alarm event integration tests
- OpenAPI tests
- observability tests and `network_node` lookup tests

Testcontainers:
- require Docker to be available
- are configured with `postgres:16-alpine`

Test profile:
- `src/test/resources/application-test.yaml`
- `app.seed.enabled=false`

