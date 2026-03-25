package pl.telco.incident.entity;

import jakarta.persistence.*;
import pl.telco.incident.entity.enums.IncidentNodeRole;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "incident_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_incident_node", columnNames = {"incident_id", "network_node_id"})
        }
)
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

    public IncidentNode() {
    }

    public IncidentNode(Long id, Incident incident, NetworkNode networkNode, IncidentNodeRole role,
                        LocalDateTime createdAt) {
        this.id = id;
        this.incident = incident;
        this.networkNode = networkNode;
        this.role = role;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static IncidentNodeBuilder builder() {
        return new IncidentNodeBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public NetworkNode getNetworkNode() {
        return networkNode;
    }

    public void setNetworkNode(NetworkNode networkNode) {
        this.networkNode = networkNode;
    }

    public IncidentNodeRole getRole() {
        return role;
    }

    public void setRole(IncidentNodeRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static final class IncidentNodeBuilder {
        private Long id;
        private Incident incident;
        private NetworkNode networkNode;
        private IncidentNodeRole role;
        private LocalDateTime createdAt;

        private IncidentNodeBuilder() {
        }

        public IncidentNodeBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IncidentNodeBuilder incident(Incident incident) {
            this.incident = incident;
            return this;
        }

        public IncidentNodeBuilder networkNode(NetworkNode networkNode) {
            this.networkNode = networkNode;
            return this;
        }

        public IncidentNodeBuilder role(IncidentNodeRole role) {
            this.role = role;
            return this;
        }

        public IncidentNodeBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IncidentNode build() {
            return new IncidentNode(id, incident, networkNode, role, createdAt);
        }
    }
}
