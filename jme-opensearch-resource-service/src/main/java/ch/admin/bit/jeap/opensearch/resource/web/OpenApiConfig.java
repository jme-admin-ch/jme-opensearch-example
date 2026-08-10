package ch.admin.bit.jeap.opensearch.resource.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "JME Opensearch Example Resource",
                description = "An example resource which has its search items indexed by the index writer service",
                contact = @Contact(
                        email = "jEAP-Community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://github.com/jme-admin-ch/jme-opensearch-example"
                )
        )
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("Example Resource API")
                .pathsToMatch("/api/**", "/index-api/**")
                .packagesToScan(this.getClass().getPackageName())
                .build();
    }
}
