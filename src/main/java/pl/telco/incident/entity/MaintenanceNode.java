package pl.telco.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import pl.telco.incident.observability.TelcoAuditEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "maintenance_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_maintenance_node", columnNames = {"maintenance_window_id", "network_node_id"})
        }
)
@EntityListeners(TelcoAuditEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_window_id", nullable = false)
    private MaintenanceWindow maintenanceWindow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "network_node_id", nullable = false)
    private NetworkNode networkNode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
