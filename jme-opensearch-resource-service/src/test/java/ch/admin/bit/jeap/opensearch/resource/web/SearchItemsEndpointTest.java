package ch.admin.bit.jeap.opensearch.resource.web;

import ch.admin.bit.jeap.opensearch.resource.domain.DomainRepository;
import ch.admin.bit.jeap.opensearch.resource.domain.DomainService;
import ch.admin.bit.jeap.opensearch.resource.domain.MessagePublisher;
import ch.admin.bit.jeap.opensearch.searchitem.api.IndexTypesUtils;
import ch.admin.bit.jeap.opensearch.searchitem.api.SearchItemsController;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.ServletJeapAuthorizationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Shows a maintenance-style request for the current transit decision in a fixed schema major. The schema major
 * selects the shape of the payload; it is neither the business revision ({@code origin.version}) nor an index name.
 */
@WebMvcTest(controllers = SearchItemsController.class)
@ContextConfiguration(classes = SearchItemsEndpointTest.TestConfig.class)
@AutoConfigureMockMvc
class SearchItemsEndpointTest {

    private static final SemanticApplicationRole SEARCHITEM_READ = SemanticApplicationRole.builder()
            .system("jme").resource("searchitem").operation("read").build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DomainService domainService;
    @MockitoBean
    private MessagePublisher messagePublisher;

    @Test
    void returnsCurrentTransitDecisionInMajor1() throws Exception {
        String id = createTransitDecision();

        mockMvc.perform(currentInMajor(id, 1))
                .andExpect(status().isOk())
                .andExpect(header().string("index-type", "JmeTransitDecision"))
                .andExpect(header().string("index-major-version", "1"))
                .andExpect(header().string("index-minor-version", "0"))
                .andExpect(jsonPath("$.origin.id").value(id))
                .andExpect(jsonPath("$.origin.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.transit_decision_id").value(id))
                .andExpect(jsonPath("$.data.transit_decision_identifier").doesNotExist());
    }

    @Test
    void returnsSameCurrentTransitDecisionInMajor2() throws Exception {
        String id = createTransitDecision();

        mockMvc.perform(currentInMajor(id, 2))
                .andExpect(status().isOk())
                .andExpect(header().string("index-major-version", "2"))
                .andExpect(header().string("index-minor-version", "0"))
                .andExpect(jsonPath("$.origin.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.transit_decision_identifier").value(id))
                .andExpect(jsonPath("$.data.transit_decision_id").doesNotExist());
    }

    @Test
    void unsupportedMajorIsAClearErrorDistinctFromNotFound() throws Exception {
        String id = createTransitDecision();

        mockMvc.perform(currentInMajor(id, 4))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_INDEX_MAJOR_VERSION"));

        mockMvc.perform(currentInMajor("does-not-exist", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void callerWithoutSchemaMajorGetsTodaysBehaviour() throws Exception {
        String id = createTransitDecision();

        mockMvc.perform(get("/index-api/searchitems")
                        .param("index_type", "JmeTransitDecision")
                        .param("origin_id", id)
                        .with(authentication(token(SEARCHITEM_READ))))
                .andExpect(status().isOk())
                .andExpect(header().string("index-major-version", "3"))
                .andExpect(header().doesNotExist("index-type"));
    }

    @Test
    void requiresSearchItemReadRole() throws Exception {
        String id = createTransitDecision();
        SemanticApplicationRole otherRole = SemanticApplicationRole.builder()
                .system("jme").resource("searchitem").operation("write").build();

        mockMvc.perform(get("/index-api/searchitems")
                        .param("index_type", "JmeTransitDecision")
                        .param("origin_id", id)
                        .param("index_major_version", "1")
                        .with(authentication(token(otherRole))))
                .andExpect(status().isForbidden());
    }

    private String createTransitDecision() {
        return domainService.createAndPublishTransitDecisionV3().searchItem().origin().id();
    }

    private static MockHttpServletRequestBuilder currentInMajor(String originId, int indexMajorVersion) {
        return get("/index-api/searchitems")
                .param("index_type", "JmeTransitDecision")
                .param("origin_id", originId)
                .param("index_major_version", String.valueOf(indexMajorVersion))
                .accept(MediaType.APPLICATION_JSON)
                .with(authentication(token(SEARCHITEM_READ)));
    }

    private static JeapAuthenticationToken token(SemanticApplicationRole... roles) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(roles).build();
    }

    @Configuration
    @EnableWebSecurity
    @Import({SearchItemsController.class, IndexTypesUtils.class, DomainService.class, DomainRepository.class})
    static class TestConfig extends ServletJeapAuthorizationConfig {

        TestConfig(ApplicationContext applicationContext) {
            super("jme", applicationContext);
        }
    }
}
