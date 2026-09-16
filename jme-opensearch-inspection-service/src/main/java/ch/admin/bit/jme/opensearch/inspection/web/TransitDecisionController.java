package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.domain.SearchItemView;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV3;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ch.admin.bit.jme.opensearch.inspection.web.SearchPaging.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/transitdecisions")
@Tag(name = "TransitDecisions", description = "Inspect indexed transit decisions.")
@RequiredArgsConstructor
public class TransitDecisionController {

    private static final IndexType<JmeTransitDecisionDataV1> INDEX_TYPE =
            JmeTransitDecisionIndexTypeV1.INSTANCE;
    private static final IndexType<JmeTransitDecisionDataV2> INDEX_TYPE_V2 =
            JmeTransitDecisionIndexTypeV2.INSTANCE;
    private static final IndexType<JmeTransitDecisionDataV3> INDEX_TYPE_V3 =
            JmeTransitDecisionIndexTypeV3.INSTANCE;
    private static final String DECIDED_BY_FIELD = "data.decided_by";
    private static final String REMARKS_FIELD = "data.remarks";

    private final SearchItemClient searchItemClient;

    // No pre-authorization here on the endpoint for this example as we want to show that
    // the searchItemClient can check a user's authorization to access search items by itself.
    @Operation(summary = "List transit decisions whose 'decided_by' starts with the given value (case-insensitive), "
            + "newest first unless the caller sorts differently.",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping
    public List<SearchItemView> list(
            @RequestParam("decidedBy") String decidedBy,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        Query prefix = Query.of(q -> q.prefix(p -> p
                .field(DECIDED_BY_FIELD)
                .value(decidedBy)
                .caseInsensitive(true)));

        return searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE, INDEX_TYPE_V2, INDEX_TYPE_V3),
                prefix,
                SearchPaging.of(pageable)
        );
    }

    @Operation(summary = "List transit decisions whose remarks match the analyzed text, newest first unless the caller "
            + "sorts differently.", security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping("/by-remarks")
    public List<SearchItemView> listByRemarks(
            @RequestParam("remarks") String remarks,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        Query match = Query.of(q -> q.match(m -> m
                .field(REMARKS_FIELD)
                .query(value -> value.stringValue(remarks))
                .operator(Operator.And)));

        return searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE, INDEX_TYPE_V2, INDEX_TYPE_V3),
                match,
                SearchPaging.of(pageable)
        );
    }
}
