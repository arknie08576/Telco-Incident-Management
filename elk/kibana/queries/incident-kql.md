# Platform KQL Pack

Wszystkie zdarzenia platformowe:

```text
eventDataset : *
```

Tylko ruch HTTP:

```text
eventDataset : "http"
```

HTTP z bledami 4xx i 5xx:

```text
eventDataset : "http" and status >= 400
```

Bledy aplikacyjne backendu:

```text
eventDataset : "system" and eventCategory : "application_error"
```

Incydenty, tylko lifecycle:

```text
eventDataset : "incident" and timelineEventType : *
```

Incydenty dla jednego priorytetu:

```text
eventDataset : "incident" and priority : "CRITICAL"
```

Incydenty dla jednego regionu:

```text
eventDataset : "incident" and region : "SLASKIE"
```

Persistence wedlug tabel:

```text
eventCategory : "persistence" and tableName : *
```

Alarmy wedlug severity:

```text
eventDataset : "alarm" and severity : *
```

Maintenance wedlug statusu:

```text
eventDataset : "maintenance" and maintenanceStatus : *
```

Lookupi network node:

```text
eventDataset : "network_node" and eventCategory : "lookup"
```

Top endpointy HTTP:

```text
eventDataset : "http" and path : *
```

Jedno ID incydentu w wielu datasetach:

```text
incidentId : 42
```

Jedno request ID przez caly flow:

```text
requestId : "wklej-request-id"
```
