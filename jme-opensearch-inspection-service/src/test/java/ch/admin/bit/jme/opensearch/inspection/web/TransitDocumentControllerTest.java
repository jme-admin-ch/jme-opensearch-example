package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.auth.IndexTypeAccessDeniedException;
import ch.admin.bit.jeap.opensearch.client.domain.SearchItemTyped;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;
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
import static ch.admin.bit.jme.opensearch.inspection.web.TestData.documentItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransitDocumentController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TransitDocumentControllerTest {

    private static final IndexType<JmeTransitDocumentDataV1> INDEX_TYPE =
            JmeTransitDocumentIndexTypeV1.INSTANCE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchItemClient searchItemClient;

    // ===== LIST: GET /api/transitdocuments?goodsDescription=... =============================

    @Test
    void list_withGoodsDescriptionParam_callsSearchWithUserAuth_withPrefixQuery() throws Exception {
        SearchItemTyped<JmeTransitDocumentDataV1> item = documentItem("doc-1", BP1);
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/transitdocuments")
                        .param("goodsDescription", "F")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origin.id").value("doc-1"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Consumer<SearchRequest.Builder>> customizerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE)), queryCaptor.capture(), customizerCaptor.capture());

        Query effective = queryCaptor.getValue();
        assertThat(effective.isPrefix()).as("V3: goodsDescription must produce a prefix query").isTrue();
        PrefixQuery prefix = effective.prefix();
        assertThat(prefix.field()).isEqualTo("data.goods_description");
        assertThat(prefix.value()).isEqualTo("F");
        assertThat(prefix.caseInsensitive())
                .as("V3: prefix must be case-insensitive — 'F' must match 'Food'/'Furniture' tokens")
                .isTrue();

        SearchRequest.Builder builder = new SearchRequest.Builder().index("dummy");
        customizerCaptor.getValue().accept(builder);
        SearchRequest req = builder.query(Query.of(q -> q.matchAll(m -> m))).build();
        assertThat(req.size()).isEqualTo(20);
    }

    @Test
    void list_missingGoodsDescriptionParam_returns400() throws Exception {
        // V3: goodsDescription is mandatory.
        mockMvc.perform(get("/api/transitdocuments"))
                .andExpect(status().isBadRequest());

        verify(searchItemClient, never())
                .searchMultiVersionWithUserAuth(any(List.class), any(Query.class), any(Consumer.class));
    }

    @Test
    void list_indexTypeAccessDenied_returns403() throws Exception {
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE)), any(Query.class), any(Consumer.class)))
                .thenThrow(IndexTypeAccessDeniedException.noAuthorization(INDEX_TYPE));

        mockMvc.perform(get("/api/transitdocuments").param("goodsDescription", "F"))
                .andExpect(status().isForbidden());
    }
}
