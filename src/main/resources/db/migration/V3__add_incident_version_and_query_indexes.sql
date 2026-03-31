ALTER TABLE incident
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_incident_priority
    ON incident(priority);

CREATE INDEX idx_incident_region
    ON incident(region);

CREATE INDEX idx_incident_source_alarm_type
    ON incident(source_alarm_type);

CREATE INDEX idx_incident_acknowledged_at
    ON incident(acknowledged_at);

CREATE INDEX idx_incident_resolved_at
    ON incident(resolved_at);

CREATE INDEX idx_incident_closed_at
    ON incident(closed_at);

CREATE INDEX idx_network_node_name
    ON network_node(node_name);

CREATE INDEX idx_network_node_region
    ON network_node(region);

CREATE INDEX idx_network_node_node_type
    ON network_node(node_type);

CREATE INDEX idx_network_node_active
    ON network_node(active);
