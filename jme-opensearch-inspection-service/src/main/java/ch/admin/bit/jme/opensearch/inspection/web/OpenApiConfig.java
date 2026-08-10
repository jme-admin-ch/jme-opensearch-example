package ch.admin.bit.jme.opensearch.inspection.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "JME Opensearch Example Inspection",
                description = "An example service that inspects search items indexed by the index writer service.",
                contact = @Contact(
                        email = "jEAP-Community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://github.com/jme-admin-ch/jme-opensearch-example"
                )
        ),
        security = @SecurityRequirement(name = "OIDC")
)
@SecurityScheme(
        name = "OIDC",
        type = SecuritySchemeType.OPENIDCONNECT,
        openIdConnectUrl = "${jeap.security.oauth2.resourceserver.authorization-server.issuer}/.well-known/openid-configuration"
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("Inspection API")
                .pathsToMatch("/api/**")
                .packagesToScan(this.getClass().getPackageName())
                .build();
    }
}
