package pl.telco.incident.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final NetworkNodeRepository networkNodeRepository;

    @Bean
    public CommandLineRunner initNetworkNodes() {
        return args -> {
            if (networkNodeRepository.count() > 0) {
                return;
            }

            NetworkNode node1 = NetworkNode.builder()
                    .nodeName("CORE-RTR-WAW-01")
                    .nodeType(NodeType.ROUTER)
                    .region("Warsaw")
                    .vendor("Cisco")
                    .active(true)
                    .build();

            NetworkNode node2 = NetworkNode.builder()
                    .nodeName("RAN-GNB-WAW-01")
                    .nodeType(NodeType.G_NODE_B)
                    .region("Warsaw")
                    .vendor("Ericsson")
                    .active(true)
                    .build();

            NetworkNode node3 = NetworkNode.builder()
                    .nodeName("RAN-GNB-WAW-02")
                    .nodeType(NodeType.G_NODE_B)
                    .region("Warsaw")
                    .vendor("Nokia")
                    .active(true)
                    .build();

            NetworkNode node4 = NetworkNode.builder()
                    .nodeName("RAN-ENB-WAW-01")
                    .nodeType(NodeType.E_NODE_B)
                    .region("Warsaw")
                    .vendor("Huawei")
                    .active(true)
                    .build();

            NetworkNode node5 = NetworkNode.builder()
                    .nodeName("CORE-SBC-WAW-01")
                    .nodeType(NodeType.SBC)
                    .region("Warsaw")
                    .vendor("Oracle")
                    .active(true)
                    .build();

            networkNodeRepository.saveAll(List.of(node1, node2, node3, node4, node5));
        };
    }
}