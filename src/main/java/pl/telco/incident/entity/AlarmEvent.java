package pl.telco.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.observability.TelcoAuditEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "alarm_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_alarm_event_source_external", columnNames = {"source_system", "external_id"})
        }
)
@EntityListeners(TelcoAuditEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class AlarmEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_node_id", nullable = false)
    private NetworkNode networkNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(name = "alarm_type", nullable = false, length = 50)
    private String alarmType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlarmSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlarmStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "suppressed_by_maintenance", nullable = false)
    private Boolean suppressedByMaintenance;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (suppressedByMaintenance == null) {
            suppressedByMaintenance = false;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
