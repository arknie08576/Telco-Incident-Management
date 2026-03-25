package pl.telco.incident.entity;

import jakarta.persistence.*;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_number", nullable = false, unique = true, length = 50)
    private String incidentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_node_id")
    private NetworkNode rootNode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private IncidentPriority priority;

    @Column(name = "source_alarm_type", length = 50)
    private String sourceAlarmType;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "is_possibly_planned", nullable = false)
    private Boolean possiblyPlanned;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentNode> incidentNodes = new ArrayList<>();

    public Incident() {
    }

    public Incident(Long id, String incidentNumber, NetworkNode rootNode, String title, IncidentStatus status,
                    IncidentPriority priority, String sourceAlarmType, String region, Boolean possiblyPlanned,
                    LocalDateTime openedAt, LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                    LocalDateTime createdAt, LocalDateTime updatedAt, List<IncidentNode> incidentNodes) {
        this.id = id;
        this.incidentNumber = incidentNumber;
        this.rootNode = rootNode;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.sourceAlarmType = sourceAlarmType;
        this.region = region;
        this.possiblyPlanned = possiblyPlanned;
        this.openedAt = openedAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.incidentNodes = incidentNodes != null ? incidentNodes : new ArrayList<>();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (openedAt == null) {
            openedAt = now;
        }
        if (status == null) {
            status = IncidentStatus.OPEN;
        }
        if (possiblyPlanned == null) {
            possiblyPlanned = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addIncidentNode(IncidentNode incidentNode) {
        incidentNodes.add(incidentNode);
        incidentNode.setIncident(this);
    }

    public static IncidentBuilder builder() {
        return new IncidentBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public NetworkNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(NetworkNode rootNode) {
        this.rootNode = rootNode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getSourceAlarmType() {
        return sourceAlarmType;
    }

    public void setSourceAlarmType(String sourceAlarmType) {
        this.sourceAlarmType = sourceAlarmType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getPossiblyPlanned() {
        return possiblyPlanned;
    }

    public void setPossiblyPlanned(Boolean possiblyPlanned) {
        this.possiblyPlanned = possiblyPlanned;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<IncidentNode> getIncidentNodes() {
        return incidentNodes;
    }

    public void setIncidentNodes(List<IncidentNode> incidentNodes) {
        this.incidentNodes = incidentNodes;
    }

    public static final class IncidentBuilder {
        private Long id;
        private String incidentNumber;
        private NetworkNode rootNode;
        private String title;
        private IncidentStatus status;
        private IncidentPriority priority;
        private String sourceAlarmType;
        private String region;
        private Boolean possiblyPlanned;
        private LocalDateTime openedAt;
        private LocalDateTime acknowledgedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<IncidentNode> incidentNodes = new ArrayList<>();

        private IncidentBuilder() {
        }

        public IncidentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IncidentBuilder incidentNumber(String incidentNumber) {
            this.incidentNumber = incidentNumber;
            return this;
        }

        public IncidentBuilder rootNode(NetworkNode rootNode) {
            this.rootNode = rootNode;
            return this;
        }

        public IncidentBuilder title(String title) {
            this.title = title;
            return this;
        }

        public IncidentBuilder status(IncidentStatus status) {
            this.status = status;
            return this;
        }

        public IncidentBuilder priority(IncidentPriority priority) {
            this.priority = priority;
            return this;
        }

        public IncidentBuilder sourceAlarmType(String sourceAlarmType) {
            this.sourceAlarmType = sourceAlarmType;
            return this;
        }

        public IncidentBuilder region(String region) {
            this.region = region;
            return this;
        }

        public IncidentBuilder possiblyPlanned(Boolean possiblyPlanned) {
            this.possiblyPlanned = possiblyPlanned;
            return this;
        }

        public IncidentBuilder openedAt(LocalDateTime openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public IncidentBuilder acknowledgedAt(LocalDateTime acknowledgedAt) {
            this.acknowledgedAt = acknowledgedAt;
            return this;
        }

        public IncidentBuilder resolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public IncidentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IncidentBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public IncidentBuilder incidentNodes(List<IncidentNode> incidentNodes) {
            this.incidentNodes = incidentNodes != null ? incidentNodes : new ArrayList<>();
            return this;
        }

        public Incident build() {
            return new Incident(id, incidentNumber, rootNode, title, status, priority, sourceAlarmType, region,
                    possiblyPlanned, openedAt, acknowledgedAt, resolvedAt, createdAt, updatedAt, incidentNodes);
        }
    }
}
