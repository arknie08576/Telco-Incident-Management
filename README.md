# Telco Incident Management

Backend symulujacy system NOC (Network Operations Center) do obslugi incydentow sieciowych w srodowisku telco.

Projekt koncentruje sie obecnie na domenie `incident`, ale ma juz przygotowane fundamenty pod:
- korelacje z `network_node`
- integracje z `alarm_event`
- awareness wzgledem `maintenance_window`

## Zakres projektu

Aktualnie zaimplementowany i przetestowany backend incidentow obejmuje:
- tworzenie incydentu
- pobranie detalu
- liste z filtrowaniem, sortowaniem i paginacja
- lookup `network_node` pod frontend i formularze incidentow
- czesciowa edycje pol biznesowych
- lifecycle incidentu
- timeline zdarzen
- OpenAPI / Swagger UI
- stabilny DTO response dla paginacji
- global exception handling
- `spring.jpa.open-in-view=false`

Obslugiwane endpointy:
- `POST /api/incidents`
- `GET /api/incidents`
- `GET /api/incidents/{id}`
- `PATCH /api/incidents/{id}`
- `PATCH /api/incidents/{id}/acknowledge`
- `PATCH /api/incidents/{id}/resolve`
- `PATCH /api/incidents/{id}/close`
- `GET /api/incidents/{id}/timeline`
- `GET /api/network-nodes`

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
- Elasticsearch + Logstash + Kibana (opcjonalnie, lokalnie)

## Architektura w repo

Glowny kod aplikacji:
- `src/main/java/pl/telco/incident/controller` - REST API
- `src/main/java/pl/telco/incident/service` - logika biznesowa
- `src/main/java/pl/telco/incident/repository` - repozytoria JPA i specifications
- `src/main/java/pl/telco/incident/entity` - encje domenowe
- `src/main/java/pl/telco/incident/dto` - requesty i response'y API
- `src/main/java/pl/telco/incident/exception` - bledy API i handler
- `src/main/java/pl/telco/incident/config` - Swagger i seed danych

Baza danych:
- `src/main/resources/db/migration/V1__init.sql`
- `src/main/resources/db/migration/V2__add_closed_at_to_incident.sql`
- `src/main/resources/db/migration/V3__add_incident_version_and_query_indexes.sql`

Testy:
- `src/test/java` - unit, WebMvc, integracyjne i OpenAPI
- `src/test/resources/application-test.yaml` - testowa konfiguracja

## Model domeny incidentu

Incident reprezentuje operacyjny problem w sieci telco. Na poziomie API i modelu ma obecnie:
- identyfikator techniczny `id`
- numer operatorski `incidentNumber`
- `title`
- `priority`
- `status`
- `region`
- `sourceAlarmType`
- `possiblyPlanned`
- `rootNodeId`
- liste powiazanych node'ow
- timestampy lifecycle:
  - `openedAt`
  - `acknowledgedAt`
  - `resolvedAt`
  - `closedAt`

Powiazany timeline przechowuje eventy biznesowe, np.:
- `CREATED`
- `UPDATED`
- `ACKNOWLEDGED`
- `RESOLVED`
- `CLOSED`

## Lifecycle

Dozwolone przejscia statusu:
- `OPEN -> ACKNOWLEDGED`
- `ACKNOWLEDGED -> RESOLVED`
- `RESOLVED -> CLOSED`

Kazda akcja lifecycle:
- aktualizuje status
- ustawia odpowiedni timestamp
- opcjonalnie zapisuje note
- dopisuje wpis do timeline

## Uruchomienie lokalne

### Wymagania

- Java 21
- Docker Desktop albo lokalny PostgreSQL
- Maven Wrapper z repo

### Pelny stack w Docker Compose

Jesli chcesz uruchomic caly backend stack w kontenerach, wystarczy:

```powershell
docker compose up -d --build
```

Wazne:
- nie uruchamiaj jednoczesnie `docker-compose.yml` i `docker-compose.elk.yml`
- `docker-compose.yml` zawiera juz w sobie `elasticsearch`, `logstash` i `kibana`
- jesli wczesniej odpaliles osobny ELK, najpierw go zatrzymaj:

```powershell
docker compose -f docker-compose.elk.yml down
```

Ten compose uruchamia:
- `postgres`
- `backend`
- `elasticsearch`
- `logstash`
- `kibana`

Dostep po starcie:
- backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

Backend w kontenerze:
- laczy sie z `postgres` po sieci Compose
- uruchamia profile `dev,elk`
- wysyla logi do `logstash`
- uruchamia Flyway przy starcie

Jesli lokalnie masz juz zajete porty, mozesz nadpisac mapowanie bez edycji pliku:

```powershell
$env:POSTGRES_PORT=55432
docker compose up -d --build
```

Analogicznie mozna nadpisac:
- `BACKEND_PORT`
- `POSTGRES_PORT`
- `ELASTICSEARCH_PORT`
- `KIBANA_PORT`
- `LOGSTASH_PORT`
- `LOGSTASH_MONITORING_PORT`

Zatrzymanie stacka:

```powershell
docker compose down
```

Jesli chcesz wyczyscic tez wolumeny danych:

```powershell
docker compose down -v
```

### Baza danych

Domyslna konfiguracja developerska jest w `src/main/resources/application.yaml`:

- URL: `jdbc:postgresql://localhost:5432/telco_incident_db`
- user: `postgres`
- port aplikacji: `8080`
- haslo bazy ustaw przez `SPRING_DATASOURCE_PASSWORD`

Minimalny setup lokalnego PostgreSQL:

```sql
CREATE DATABASE telco_incident_db;
```

Flyway uruchamia migracje automatycznie przy starcie aplikacji.

Jesli nie chcesz uzywac domyslnej konfiguracji, zmien `application.yaml` albo nadpisz property przy starcie aplikacji.

### Start aplikacji

Tryb developerski:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Build i uruchomienie jar:

```powershell
.\mvnw clean package
java -jar target/telco-incident-management-0.0.1-SNAPSHOT.jar
```

### Swagger / OpenAPI

Po uruchomieniu aplikacji:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Seed danych

Seeder jest wylaczony domyslnie poza profilem `dev` i uruchamia sie tylko wtedy, gdy tabele sa puste.

Konfiguracja:
- property: `app.seed.enabled`
- domyslnie: `false`
- profil `dev`: `true`
- w testach: `false`

Seeder dodaje przykladowe:
- network nodes
- incidents
- maintenance windows
- alarm events
- incident timeline entries

Implementacja: `src/main/java/pl/telco/incident/config/DataInitializer.java`

Przyklad wylaczenia seeda:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-Dapp.seed.enabled=false"
```

## Observability i ELK

Aplikacja ma przygotowane podstawy pod centralizacje logow:
- `X-Request-Id` dla kazdego requestu
- logowanie request/response na warstwie HTTP
- logi biznesowe dla create, update i lifecycle incidentow
- logowanie bledow w `GlobalExceptionHandler`
- profil `elk`, ktory wysyla logi JSON bezposrednio do Logstash po TCP
- Spring Boot Actuator dla darmowych endpointow observability
- metryki biznesowe lifecycle i zmian incidentow przez Micrometer

### Actuator

Dostepne endpointy:
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/info`
- `GET /actuator/metrics`

To jest celowo lekki, darmowy pakiet observability dobry do projektu studenckiego:
- healthcheck pod lokalne uruchomienie i Docker
- podstawowe metadane aplikacji
- metryki runtime bez wchodzenia w platne funkcje platformowe
- liczniki i timery biznesowe dla incident lifecycle

### Uruchomienie lokalnego stacka ELK

Jesli chcesz uruchomic tylko ELK bez backendu i bez Postgresa, nadal mozesz uzyc lekkiego compose:

```powershell
docker compose -f docker-compose.elk.yml up -d
```

Ten wariant jest alternatywa dla pelnego `docker compose up -d --build`, a nie dodatkiem do niego.

Start kontenerow:

```powershell
docker compose -f docker-compose.elk.yml up -d
```

Dostepne endpointy:
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- Logstash TCP input: `localhost:5000`

### Start aplikacji z profilem ELK

```powershell
$env:SPRING_DATASOURCE_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev,elk"
```

Po starcie aplikacji logi trafia do Logstash, a potem do Elasticsearch do indeksu:

```text
telco-incident-management-YYYY.MM.dd
```

W Kibanie warto zalozyc data view:

```text
telco-incident-management-*
```

Przykladowe pola w logach:
- `requestId`
- `method`
- `path`
- `status`
- `durationMs`
- `eventDataset`
- `eventCategory`
- `eventAction`
- `timelineEventType`
- `incidentId`
- `incidentNumber`
- `incidentStatus`
- `priority`
- `region`
- `possiblyPlanned`
- `service`

Przykladowe metryki biznesowe:
- `incident.created`
- `incident.updated`
- `incident.lifecycle.transition`
- `incident.time.to_ack`
- `incident.time.to_resolve`
- `incident.time.to_close`

### Kibana starter pack

Repo zawiera prosty starter do Kibany:
- importowalny dashboard: `elk/kibana/saved-objects/telco-incident-observability.ndjson`
- skrypt importu: `elk/kibana/import-saved-objects.ps1`
- generator ruchu demo do API: `elk/demo/generate-dashboard-data.ps1`
- paczka gotowych KQL: `elk/kibana/queries/incident-kql.md`

Import dashboardu:

```powershell
.\elk\kibana\import-saved-objects.ps1
```

### Szybkie nakarmienie dashboardu danymi

Jesli chcesz szybko zobaczyc niepuste panele w Kibanie, uruchom generator ruchu:

```powershell
.\elk\demo\generate-dashboard-data.ps1
```

Domyslnie skrypt:
- zaklada lokalny backend pod `http://localhost:8080`
- korzysta z seed node'ow `1` jako `ROOT` oraz `2,3` jako `AFFECTED`
- tworzy kilka incidentow
- wykonuje `update`, `acknowledge`, `resolve`, `close`
- dorzuca kilka `GET` do listy, detalu i timeline

Jesli masz inne ID node'ow albo inny adres aplikacji:

```powershell
.\elk\demo\generate-dashboard-data.ps1 -BaseUrl http://localhost:8080 -RootNodeId 1 -AffectedNodeIds 2,3 -IncidentCount 4
```

Po odpaleniu skryptu ustaw w Kibanie zakres czasu na `Last 24 hours` i kliknij `Refresh`.

### Szybki scenariusz demo

Najprostszy flow do pokazania projektu na demo:

1. Uruchom pelny stack:

```powershell
docker compose up -d --build
```

2. Zaimportuj dashboard do Kibany:

```powershell
.\elk\kibana\import-saved-objects.ps1
```

3. Wygeneruj ruch do API:

```powershell
.\elk\demo\generate-dashboard-data.ps1
```

4. Otworz:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Kibana: `http://localhost:5601`

5. W Kibanie otworz dashboard `Telco Incident Observability`, ustaw `Last 24 hours` i kliknij `Refresh`.

Po imporcie w Kibanie pojawi sie dashboard:

```text
Telco Incident Observability
```

To nie jest rozbudowany dashboard produkcyjny. To lekki starter pod projekt studencki:
- metric z laczna liczba eventow incidentow
- metric z bledami HTTP dla incident API
- rozklad po `incidentStatus`
- rozklad po `priority`
- eventy w czasie
- rozklad po `eventAction`
- tabele ostatnich eventow do szybkiego drilldownu

## Incident API

### Create incident

`POST /api/incidents`

Przykladowy request:

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

Reguly biznesowe:
- `incidentNumber` musi byc unikalny
- `nodes` nie moga zawierac duplikatow `networkNodeId`
- dokladnie jeden node musi miec role `ROOT`
- `rootNodeId` musi wskazywac ten sam node co rekord z rola `ROOT`

### Get incident

`GET /api/incidents/{id}`

Zwraca detal incidentu:
- aktualny status i timestampy lifecycle
- `rootNodeId`
- `sourceAlarmType`
- `possiblyPlanned`
- liste powiazanych node'ow z rolami i podstawowymi danymi inventory

### Update incident

`PATCH /api/incidents/{id}`

Endpoint wspiera partial update pol biznesowych:
- `incidentNumber`
- `title`
- `priority`
- `region`
- `sourceAlarmType`
- `possiblyPlanned`

Celowo nie wspiera:
- zmiany statusu
- zmiany `rootNodeId`
- zmiany listy `nodes`

Reguly:
- incident `CLOSED` nie moze byc edytowany
- pusty albo no-op patch zwraca `400`
- zmiana `incidentNumber` pilnuje unikalnosci
- udany update dopisuje event `UPDATED` do timeline

Przykladowy request:

```json
{
  "title": "Updated incident title",
  "priority": "CRITICAL",
  "sourceAlarmType": "POWER"
}
```

### Lifecycle endpoints

- `PATCH /api/incidents/{id}/acknowledge`
- `PATCH /api/incidents/{id}/resolve`
- `PATCH /api/incidents/{id}/close`

Body moze zawierac opcjonalna notatke:

```json
{
  "note": "Traffic rerouted"
}
```

### Timeline

`GET /api/incidents/{id}/timeline`

Zwraca eventy w kolejnosci rosnacej po `createdAt`.

### Network node lookup

`GET /api/network-nodes`

Endpoint pomocniczy pod frontend i formularze create/update.

Wspiera filtry:
- `q` - case-insensitive partial match po `nodeName`
- `region`
- `nodeType`
- `active`

## Incident listing

`GET /api/incidents`

Lista zwraca stabilny DTO zamiast surowego `PageImpl`:

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

`content` listy zawiera summary incidentu. Detail z node'ami i dodatkowymi polami jest zwracany przez `GET /api/incidents/{id}` oraz odpowiedzi mutujace.

### Paginacja

- `page` - zero-based page index
- `size` - od `1` do `100`

### Sortowanie

Parametry:
- `sortBy`
- `direction`

Obslugiwane pola sortowania:
- `openedAt`
- `acknowledgedAt`
- `resolvedAt`
- `closedAt`
- `incidentNumber`
- `priority`
- `title`

Dla `acknowledgedAt`, `resolvedAt` i `closedAt` sortowanie obsluguje `NULLS_LAST`.

### Filtry

Pojedyncze filtry:
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

Filtry wielowartosciowe:
- `priorities`
- `statuses`

`priorities` i `statuses` wspieraja:
- parametry powtarzalne
- wartosci comma-separated

Zakresy dat przyjmuja format ISO-8601, np. `2026-03-29T10:15:00`.

`region` i `sourceAlarmType` sa typami domenowymi i przyjmuja wartosci enum, np. `MAZOWIECKIE`, `HARDWARE`.

Przykladowe requesty:

```text
GET /api/incidents?status=OPEN
GET /api/incidents?statuses=OPEN,ACKNOWLEDGED
GET /api/incidents?priorities=HIGH,CRITICAL&sortBy=closedAt&direction=desc
GET /api/incidents?incidentNumber=INC-10&title=router
GET /api/incidents?openedFrom=2026-03-29T08:00:00&openedTo=2026-03-29T12:00:00
GET /api/incidents?closedFrom=2026-03-29T10:00:00&closedTo=2026-03-29T12:00:00
```

### Walidacja listy

API zwraca `400`, gdy:
- `sortBy` jest nieobslugiwane
- `direction` jest nieobslugiwane
- enumy w filtrach sa niepoprawne
- dowolny zakres dat ma `from > to`
- `page` albo `size` sa poza dozwolonym zakresem

## Przykladowe wywolania curl

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

## Obsluga bledow

API ma globalny exception handling i zwraca przewidywalne odpowiedzi dla najczestszych klas bledow:
- `400 Bad Request` - walidacja requestu, zle query params, zly lifecycle, empty patch
- `404 Not Found` - brak incidentu albo referencji do node'a
- `409 Conflict` - konflikt biznesowy, np. zajety `incidentNumber`

Kluczowy plik: `src/main/java/pl/telco/incident/exception/GlobalExceptionHandler.java`

## Testy

Uruchomienie wszystkich testow:

```powershell
.\mvnw test
```

Zakres testow:
- unit testy serwisu incidentow
- WebMvc testy kontrolera i exception handling
- integracyjne testy API na PostgreSQL przez Testcontainers
- testy OpenAPI
- testy observability oraz lookup `network_node`

Testcontainers:
- wymagaja dostepnego Dockera
- sa skonfigurowane z `postgres:16-alpine`

Profil testowy:
- `src/test/resources/application-test.yaml`
- `app.seed.enabled=false`

## Kluczowe pliki

- `src/main/java/pl/telco/incident/controller/IncidentController.java`
- `src/main/java/pl/telco/incident/controller/NetworkNodeController.java`
- `src/main/java/pl/telco/incident/service/IncidentService.java`
- `src/main/java/pl/telco/incident/service/NetworkNodeService.java`
- `src/main/java/pl/telco/incident/repository/specification/IncidentSpecifications.java`
- `src/main/java/pl/telco/incident/dto/IncidentUpdateRequest.java`
- `src/main/java/pl/telco/incident/config/OpenApiConfig.java`
- `src/main/resources/db/migration/V3__add_incident_version_and_query_indexes.sql`
- `src/test/java/pl/telco/incident/IncidentApiIntegrationTest.java`
- `src/test/java/pl/telco/incident/controller/IncidentControllerWebMvcTest.java`
- `src/test/java/pl/telco/incident/service/IncidentServiceTest.java`

## Aktualny status

Backend incidentow ma juz sensowny pion end-to-end:
- CRUD-lite: create, get, list, partial update
- lifecycle z note'ami i timestampami
- timeline
- dokumentacje OpenAPI
- test foundation z integracjami na PostgreSQL

Najbardziej naturalne kolejne kierunki rozwoju:
- frontend incidentow oparty o obecny kontrakt API
- observability i metryki lifecycle
- dalsze rozszerzanie domeny alarmow i maintenance
