package ch.admin.bit.jme.opensearch.inspection;

import ch.admin.bit.jeap.opensearch.client.auth.Authorization;
import ch.admin.bit.jeap.opensearch.client.auth.UserAuthorizationProvider;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Import(InspectionApplicationIT.TestSecurityConfiguration.class)
@Testcontainers
class InspectionApplicationIT {

    private static final String DOCUMENT_ID = "integration-test-document";
    private static final String GOODS_DESCRIPTION = "Portable OpenSearch example";
    private static final List<String> KEYWORDS = List.of("portable", "example");

    @Container
    static final OpensearchContainer<?> OPENSEARCH =
            new OpensearchContainer<>("opensearchproject/opensearch:3.3.2");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.opensearch.client.connection.uri", OPENSEARCH::getHttpHostAddress);
        registry.add("jeap.web.tls.enabled", () -> false);
    }

    @BeforeAll
    static void createIndex(@Autowired OpenSearchClient openSearchClient) throws IOException {
        String index = JmeTransitDocumentIndexTypeV1.INSTANCE.indexReadAlias();
        openSearchClient.indices().create(request -> request
                .index(index)
                .mappings(mapping -> mapping
                        .properties("origin", property -> property.object(origin -> origin
                                .properties("id", field -> field.keyword(keyword -> keyword))
                                .properties("version", field -> field.keyword(keyword -> keyword))
                                .properties("bp_id", field -> field.keyword(keyword -> keyword))
                                .properties("tenant", field -> field.keyword(keyword -> keyword))))
                        .properties("search_item", property -> property.object(metadata -> metadata
                                .properties("major_version", field -> field.integer(integer -> integer))
                                .properties("minor_version", field -> field.integer(integer -> integer))))
                        .properties("data", property -> property.object(data -> data
                                .properties("transit_document_id", field -> field.keyword(keyword -> keyword))
                                .properties("keywords", field -> field.keyword(keyword -> keyword))
                                .properties("goods_description", field -> field.text(text -> text))))));

        openSearchClient.index(request -> request
                .index(index)
                .id(DOCUMENT_ID)
                .refresh(Refresh.True)
                .document(Map.of(
                        "origin", Map.of(
                                "id", DOCUMENT_ID,
                                "version", "1.0",
                                "bp_id", "BP1",
                                "tenant", "TENANT1",
                                "created", Instant.parse("2026-01-01T00:00:00Z").toString(),
                                "modified", Instant.parse("2026-01-01T00:00:00Z").toString()),
                        "search_item", Map.of("major_version", 1, "minor_version", 3),
                        "data", Map.of(
                                "transit_document_id", DOCUMENT_ID,
                                "keywords", KEYWORDS,
                                "goods_description", GOODS_DESCRIPTION))));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsApplicationAndSearchesRealOpenSearchIndex() throws Exception {
        mockMvc.perform(get("/api/transitdocuments").param("goodsDescription", "portable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origin.id").value(DOCUMENT_ID))
                .andExpect(jsonPath("$[0].data.keywords[0]").value(KEYWORDS.get(0)))
                .andExpect(jsonPath("$[0].data.keywords[1]").value(KEYWORDS.get(1)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityConfiguration {

        @Bean
        UserAuthorizationProvider userAuthorizationProvider() {
            return () -> new Authorization(Set.of("jme_read"), Map.of());
        }

    }
}
