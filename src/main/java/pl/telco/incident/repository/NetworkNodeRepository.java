package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.NetworkNode;

import java.util.Optional;

public interface NetworkNodeRepository extends JpaRepository<NetworkNode, Long> {
    Optional<NetworkNode> findByNodeName(String nodeName);
    boolean existsByNodeName(String nodeName);
}