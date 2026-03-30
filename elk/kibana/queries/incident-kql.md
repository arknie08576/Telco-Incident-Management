# Incident KQL Pack

Dla przekrojowego CRUD wszystkich tabel zobacz tez `crud-kql.md`.

Wszystkie logi biznesowe incidentow:

```text
eventDataset : "incident"
```

Tylko zamkniecia incidentow:

```text
eventDataset : "incident" and eventAction : "close"
```

Zdarzenia dla jednego numeru incidentu:

```text
incidentNumber : "INC-103"
```

Incidenty krytyczne w danym regionie:

```text
eventDataset : "incident" and priority : "CRITICAL" and region : "MAZOWIECKIE"
```

Operacje z action note:

```text
eventDataset : "incident" and noteProvided : true
```

Bledy HTTP dla incident API:

```text
path : "/api/incidents" and status >= 400
```

Zmiany biznesowe w patch update:

```text
eventDataset : "incident" and eventAction : "update"
```
