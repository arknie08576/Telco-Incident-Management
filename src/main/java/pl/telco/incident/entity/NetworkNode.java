package pl.telco.incident.entity;

import jakarta.persistence.*;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "network_node")
public class NetworkNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 100)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 50)
    private NodeType nodeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 100)
    private Region region;

    @Column(name = "vendor", length = 100)
    private String vendor;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "rootNode")
    private List<Incident> rootIncidents = new ArrayList<>();

    public NetworkNode() {
    }

    public NetworkNode(Long id, String nodeName, NodeType nodeType, Region region, String vendor, Boolean active,
                       LocalDateTime createdAt, List<Incident> rootIncidents) {
        this.id = id;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.region = region;
        this.vendor = vendor;
        this.active = active;
        this.createdAt = createdAt;
        this.rootIncidents = rootIncidents != null ? rootIncidents : new ArrayList<>();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }

    public static NetworkNodeBuilder builder() {
        return new NetworkNodeBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Incident> getRootIncidents() {
        return rootIncidents;
    }

    public void setRootIncidents(List<Incident> rootIncidents) {
        this.rootIncidents = rootIncidents;
    }

    public static final class NetworkNodeBuilder {
        private Long id;
        private String nodeName;
        private NodeType nodeType;
        private Region region;
        private String vendor;
        private Boolean active;
        private LocalDateTime createdAt;
        private List<Incident> rootIncidents = new ArrayList<>();

        private NetworkNodeBuilder() {
        }

        public NetworkNodeBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public NetworkNodeBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public NetworkNodeBuilder nodeType(NodeType nodeType) {
            this.nodeType = nodeType;
            return this;
        }

        public NetworkNodeBuilder region(Region region) {
            this.region = region;
            return this;
        }

        public NetworkNodeBuilder vendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        public NetworkNodeBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public NetworkNodeBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NetworkNodeBuilder rootIncidents(List<Incident> rootIncidents) {
            this.rootIncidents = rootIncidents != null ? rootIncidents : new ArrayList<>();
            return this;
        }

        public NetworkNode build() {
            return new NetworkNode(id, nodeName, nodeType, region, vendor, active, createdAt, rootIncidents);
        }
    }
}
