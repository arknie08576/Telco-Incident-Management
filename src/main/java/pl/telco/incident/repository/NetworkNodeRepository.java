package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.telco.incident.entity.NetworkNode;

import java.util.Optional;

public interface NetworkNodeRepository extends JpaRepository<NetworkNode, Long>, JpaSpecificationExecutor<NetworkNode> {
    Optional<NetworkNode> findByNodeName(String nodeName);
    boolean existsByNodeName(String nodeName);
}
