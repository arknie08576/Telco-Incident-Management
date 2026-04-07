package pl.telco.incident.entity;

import jakarta.persistence.*;
import lombok.*;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.observability.TelcoAuditEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "incident_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_incident_node", columnNames = {"incident_id", "network_node_id"})
        }
)
@EntityListeners(TelcoAuditEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_node_id", nullable = false)
    private NetworkNode networkNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private IncidentNodeRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
