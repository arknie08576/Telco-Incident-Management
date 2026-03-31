package pl.telco.incident.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Telco Incident Management API",
                version = "v1",
                description = "REST API for creating, tracking and progressing telco incidents.",
                contact = @Contact(name = "Telco Incident Management"),
                license = @License(name = "Internal Use Only")
        ),
        tags = {
                @Tag(
                        name = "Incidents",
                        description = "Incident creation, lifecycle operations, timeline and filtered listing."
                ),
                @Tag(
                        name = "Network Nodes",
                        description = "Inventory lookup endpoints used by incident workflows."
                )
        }
)
public class OpenApiConfig {
}
