CREATE TABLE network_node (
                              id BIGSERIAL PRIMARY KEY,
                              node_name VARCHAR(100) NOT NULL,
                              node_type VARCHAR(50) NOT NULL,
                              region VARCHAR(100) NOT NULL,
                              vendor VARCHAR(100),
                              active BOOLEAN NOT NULL DEFAULT TRUE,
                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                              CONSTRAINT uq_network_node_name UNIQUE (node_name)
);

CREATE TABLE incident (
                          id BIGSERIAL PRIMARY KEY,
                          incident_number VARCHAR(50) NOT NULL,
                          root_node_id BIGINT,
                          title VARCHAR(255) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          priority VARCHAR(30) NOT NULL,
                          source_alarm_type VARCHAR(50),
                          region VARCHAR(100) NOT NULL,
                          is_possibly_planned BOOLEAN NOT NULL DEFAULT FALSE,
                          opened_at TIMESTAMP NOT NULL,
                          acknowledged_at TIMESTAMP,
                          resolved_at TIMESTAMP,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT uq_incident_number UNIQUE (incident_number),
                          CONSTRAINT fk_incident_root_node
                              FOREIGN KEY (root_node_id) REFERENCES network_node(id)
);

CREATE TABLE incident_node (
                               id BIGSERIAL PRIMARY KEY,
                               incident_id BIGINT NOT NULL,
                               network_node_id BIGINT NOT NULL,
                               role VARCHAR(30) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_incident_node_incident
                                   FOREIGN KEY (incident_id) REFERENCES incident(id) ON DELETE CASCADE,
                               CONSTRAINT fk_incident_node_network_node
                                   FOREIGN KEY (network_node_id) REFERENCES network_node(id),
                               CONSTRAINT uq_incident_node UNIQUE (incident_id, network_node_id)
);

CREATE TABLE maintenance_window (
                                    id BIGSERIAL PRIMARY KEY,
                                    title VARCHAR(255) NOT NULL,
                                    description TEXT,
                                    status VARCHAR(30) NOT NULL,
                                    start_time TIMESTAMP NOT NULL,
                                    end_time TIMESTAMP NOT NULL,
                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                    CONSTRAINT chk_maintenance_window_time
                                        CHECK (end_time > start_time)
);

CREATE TABLE maintenance_node (
                                  id BIGSERIAL PRIMARY KEY,
                                  maintenance_window_id BIGINT NOT NULL,
                                  network_node_id BIGINT NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                  CONSTRAINT fk_maintenance_node_window
                                      FOREIGN KEY (maintenance_window_id) REFERENCES maintenance_window(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_maintenance_node_network_node
                                      FOREIGN KEY (network_node_id) REFERENCES network_node(id),
                                  CONSTRAINT uq_maintenance_node UNIQUE (maintenance_window_id, network_node_id)
);

CREATE TABLE alarm_event (
                             id BIGSERIAL PRIMARY KEY,
                             source_system VARCHAR(50) NOT NULL,
                             external_id VARCHAR(100) NOT NULL,
                             network_node_id BIGINT NOT NULL,
                             incident_id BIGINT,
                             alarm_type VARCHAR(50) NOT NULL,
                             severity VARCHAR(20) NOT NULL,
                             status VARCHAR(30) NOT NULL,
                             description TEXT,
                             suppressed_by_maintenance BOOLEAN NOT NULL DEFAULT FALSE,
                             occurred_at TIMESTAMP NOT NULL,
                             received_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_alarm_event_network_node
                                 FOREIGN KEY (network_node_id) REFERENCES network_node(id),
                             CONSTRAINT fk_alarm_event_incident
                                 FOREIGN KEY (incident_id) REFERENCES incident(id),
                             CONSTRAINT uq_alarm_event_source_external UNIQUE (source_system, external_id)
);

CREATE TABLE incident_timeline (
                                   id BIGSERIAL PRIMARY KEY,
                                   incident_id BIGINT NOT NULL,
                                   event_type VARCHAR(50) NOT NULL,
                                   message TEXT NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                   CONSTRAINT fk_incident_timeline_incident
                                       FOREIGN KEY (incident_id) REFERENCES incident(id) ON DELETE CASCADE
);

CREATE INDEX idx_incident_root_node_id
    ON incident(root_node_id);

CREATE INDEX idx_incident_status
    ON incident(status);

CREATE INDEX idx_incident_opened_at
    ON incident(opened_at);

CREATE INDEX idx_incident_node_incident_id
    ON incident_node(incident_id);

CREATE INDEX idx_incident_node_network_node_id
    ON incident_node(network_node_id);

CREATE INDEX idx_maintenance_window_status
    ON maintenance_window(status);

CREATE INDEX idx_maintenance_window_time_range
    ON maintenance_window(start_time, end_time);

CREATE INDEX idx_maintenance_node_window_id
    ON maintenance_node(maintenance_window_id);

CREATE INDEX idx_maintenance_node_network_node_id
    ON maintenance_node(network_node_id);

CREATE INDEX idx_alarm_event_network_node_id
    ON alarm_event(network_node_id);

CREATE INDEX idx_alarm_event_incident_id
    ON alarm_event(incident_id);

CREATE INDEX idx_alarm_event_occurred_at
    ON alarm_event(occurred_at);

CREATE INDEX idx_alarm_event_alarm_type
    ON alarm_event(alarm_type);

CREATE INDEX idx_alarm_event_severity
    ON alarm_event(severity);

CREATE INDEX idx_incident_timeline_incident_id
    ON incident_timeline(incident_id);

CREATE INDEX idx_incident_timeline_created_at
    ON incident_timeline(created_at);