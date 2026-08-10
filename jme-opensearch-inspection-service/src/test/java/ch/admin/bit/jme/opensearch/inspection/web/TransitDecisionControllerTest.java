package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.auth.IndexTypeAccessDeniedException;
import ch.admin.bit.jeap.opensearch.client.domain.SearchItemTyped;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV2;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch._types.query_dsl.PrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.function.Consumer;

import static ch.admin.bit.jme.opensearch.inspection.web.TestData.BP1;
import static ch.admin.bit.jme.opensearch.inspection.web.TestData.decisionItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransitDecisionController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TransitDecisionControllerTest {

    private static final IndexType<JmeTransitDecisionDataV1> INDEX_TYPE =
            JmeTransitDecisionIndexTypeV1.INSTANCE;
    private static final IndexType<JmeTransitDecisionDataV2> INDEX_TYPE_V2 =
            JmeTransitDecisionIndexTypeV2.INSTANCE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchItemClient searchItemClient;

    // ===== LIST: GET /api/transitdecisions?decidedBy=... ===================================

    @Test
    void list_withDecidedByParam_callsSearchWithUserAuth_withPrefixQueryAndSize20() throws Exception {
        SearchItemTyped<JmeTransitDecisionDataV1> item = decisionItem("dec-1", BP1);
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/transitdecisions")
                        .param("decidedBy", "alice")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origin.id").value("dec-1"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Consumer<SearchRequest.Builder>> customizerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), queryCaptor.capture(), customizerCaptor.capture());

        Query effective = queryCaptor.getValue();
        assertThat(effective.isPrefix()).as("V3: decidedBy must produce a prefix query").isTrue();
        PrefixQuery prefix = effective.prefix();
        assertThat(prefix.field()).isEqualTo("data.decided_by");
        assertThat(prefix.value()).isEqualTo("alice");
        assertThat(prefix.caseInsensitive())
                .as("V3: prefix must be case-insensitive so 'A' matches 'alice'")
                .isTrue();

        SearchRequest.Builder builder = new SearchRequest.Builder().index("dummy");
        customizerCaptor.getValue().accept(builder);
        SearchRequest req = builder.query(Query.of(q -> q.matchAll(m -> m))).build();
        assertThat(req.size()).isEqualTo(20);
    }

    @Test
    void list_capitalLetterParam_isAcceptedAndForwardedCaseInsensitively() throws Exception {
        // V3 startsWith semantics example: "F" should match items whose decided_by tokens
        // start with f/F. The controller forwards the raw value; the case-insensitivity
        // flag is what makes the OpenSearch query match across cases.
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transitdecisions").param("decidedBy", "F"))
                .andExpect(status().isOk());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), queryCaptor.capture(), any(Consumer.class));

        PrefixQuery prefix = queryCaptor.getValue().prefix();
        assertThat(prefix.value()).isEqualTo("F");
        assertThat(prefix.caseInsensitive()).isTrue();
    }

    @Test
    void list_missingDecidedByParam_returns400() throws Exception {
        // V3: decidedBy is mandatory. Spring MVC produces HTTP 400 when a required
        // request parameter is missing — no search is performed.
        mockMvc.perform(get("/api/transitdecisions"))
                .andExpect(status().isBadRequest());

        verify(searchItemClient, never())
                .searchMultiVersionWithUserAuth(any(List.class), any(Query.class), any(Consumer.class));
    }

    @Test
    void list_emptyDecidedByParam_isAccepted_prefixWithEmptyString() throws Exception {
        // Empty string is syntactically valid: prefix("") matches every value.
        // We don't add a separate validation in V3; the test pins that behaviour.
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transitdecisions").param("decidedBy", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), queryCaptor.capture(), any(Consumer.class));
        assertThat(queryCaptor.getValue().prefix().value()).isEmpty();
    }

    @Test
    void list_indexTypeAccessDenied_returns403() throws Exception {
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE, INDEX_TYPE_V2)), any(Query.class), any(Consumer.class)))
                .thenThrow(IndexTypeAccessDeniedException.noAuthorization(INDEX_TYPE));

        mockMvc.perform(get("/api/transitdecisions").param("decidedBy", "alice"))
                .andExpect(status().isForbidden());
    }

}
