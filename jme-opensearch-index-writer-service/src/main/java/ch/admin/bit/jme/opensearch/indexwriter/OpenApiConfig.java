package ch.admin.bit.jme.opensearch.indexwriter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

// The index writer library publishes its maintenance job API as the OpenAPI group "maintenance-jobs" and marks it as
// protected; the service only describes itself here
@OpenAPIDefinition(
        info = @Info(
                title = "JME Opensearch Example Index Writer",
                description = "Maintenance jobs of the index writer: submit upsert and delete jobs as YAML and read their reports.",
                contact = @Contact(
                        email = "jEAP-Community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://github.com/jme-admin-ch/jme-opensearch-example"
                )
        )
)
@Configuration
class OpenApiConfig {
}
