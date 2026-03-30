# CRUD KQL Pack

Wszystkie strukturalne eventy domenowe:

```text
eventDataset : * and eventAction : *
```

Tylko CRUD dla tabel poza incident lifecycle:

```text
eventCategory : "entity_crud"
```

CRUD dla `network_node`:

```text
eventDataset : "network_node"
```

CRUD dla `maintenance_window`:

```text
eventDataset : "maintenance_window"
```

CRUD dla `alarm_event`:

```text
eventDataset : "alarm_event"
```

CRUD dla tabel relacyjnych:

```text
eventDataset : ("incident_node" or "maintenance_node" or "incident_timeline")
```

Usuniecia rekordow:

```text
eventAction : "delete"
```

Operacje powiazane z konkretnym incidentem:

```text
incidentId : 1
```

Eventy po request ID:

```text
requestId : "PUT-TUTAJ-X-REQUEST-ID"
```
