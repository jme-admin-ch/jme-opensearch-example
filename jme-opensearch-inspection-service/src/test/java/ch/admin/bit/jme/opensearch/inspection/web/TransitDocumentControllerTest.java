package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.auth.IndexTypeAccessDeniedException;
import ch.admin.bit.jeap.opensearch.client.domain.SearchItemTyped;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.PrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
    void list_withGoodsDescriptionParam_callsSearchWithUserAuth_withPrefixQueryAndDefaultPage() throws Exception {
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

        assertDefaultPageNewestFirst(capturedSearchRequest(customizerCaptor.getValue()));
    }

    @ParameterizedTest
    @MethodSource("termQueryRequests")
    void termQueryEndpoints_callSearchWithExpectedQuery(String path, String parameter, String value,
                                                        String expectedField, boolean nested) throws Exception {
        when(searchItemClient.searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of(documentItem("doc-1", BP1)));

        mockMvc.perform(get(path).param(parameter, value).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin.id").value("doc-1"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Consumer<SearchRequest.Builder>> customizerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE)), queryCaptor.capture(), customizerCaptor.capture());

        assertDefaultPageNewestFirst(capturedSearchRequest(customizerCaptor.getValue()));

        Query query = queryCaptor.getValue();
        TermQuery term;
        if (nested) {
            assertThat(query.isNested()).isTrue();
            assertThat(query.nested().path()).isEqualTo("data.customs_checks");
            assertThat(query.nested().query().isTerm()).isTrue();
            term = query.nested().query().term();
        } else {
            assertThat(query.isTerm()).isTrue();
            term = query.term();
        }
        assertThat(term.field()).isEqualTo(expectedField);
        assertThat(term.value().stringValue()).isEqualTo(value);
    }

    private static Stream<Arguments> termQueryRequests() {
        return Stream.of(
                Arguments.of("/api/transitdocuments/by-keyword", "keyword", "transit",
                        "data.keywords", false),
                Arguments.of("/api/transitdocuments/by-customs-office", "office", "Basel",
                        "data.customs_checks.office", true),
                Arguments.of("/api/transitdocuments/by-customs-tag", "tag", "inspection",
                        "data.customs_checks.tags", true),
                Arguments.of("/api/transitdocuments/by-customs-code", "code", "A1",
                        "data.customs_checks.details.codes", true));
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


    @Test
    void list_withPageableParams_forwardsPagingAndSortingToTheSearchRequest() throws Exception {
        when(searchItemClient.searchMultiVersionWithUserAuth(eq(List.of(INDEX_TYPE)), any(Query.class), any(Consumer.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transitdocuments")
                        .param("goodsDescription", "F")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "data.goods_description,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Consumer<SearchRequest.Builder>> customizerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(searchItemClient).searchMultiVersionWithUserAuth(
                eq(List.of(INDEX_TYPE)), any(Query.class), customizerCaptor.capture());

        SearchRequest req = capturedSearchRequest(customizerCaptor.getValue());
        assertThat(req.from()).isEqualTo(10);
        assertThat(req.size()).isEqualTo(5);
        assertSortedBy(req, "data.goods_description", SortOrder.Asc);
    }
    /**
     * The result is capped at a page size, so the search must impose an explicit order. Without it
     * OpenSearch returns an arbitrary slice of the matches and a freshly indexed item can stay
     * invisible forever once more documents match than fit on a page.
     */
    private static void assertDefaultPageNewestFirst(SearchRequest request) {
        assertThat(request.from()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertSortedBy(request, "origin.created", SortOrder.Desc);
    }

    private static void assertSortedBy(SearchRequest request, String field, SortOrder order) {
        assertThat(request.sort()).hasSize(1);
        FieldSort sort = request.sort().getFirst().field();
        assertThat(sort).as("search must be sorted by a field").isNotNull();
        assertThat(sort.field()).isEqualTo(field);
        assertThat(sort.order()).isEqualTo(order);
    }

    private static SearchRequest capturedSearchRequest(Consumer<SearchRequest.Builder> customizer) {
        SearchRequest.Builder builder = new SearchRequest.Builder().index("dummy");
        customizer.accept(builder);
        return builder.query(Query.of(q -> q.matchAll(m -> m))).build();
    }
}
