package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.domain.SearchItemView;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transitdecisions")
@Tag(name = "TransitDecisions", description = "Inspect indexed transit decisions.")
@RequiredArgsConstructor
public class TransitDecisionController {

    private static final int PAGE_SIZE = 20;
    private static final IndexType<JmeTransitDecisionDataV1> INDEX_TYPE =
            JmeTransitDecisionIndexTypeV1.INSTANCE;
    private static final IndexType<JmeTransitDecisionDataV2> INDEX_TYPE_V2 =
            JmeTransitDecisionIndexTypeV2.INSTANCE;
    private static final String DECIDED_BY_FIELD = "data.decided_by";

    private final SearchItemClient searchItemClient;

    // No pre-authorization here on the endpoint for this example as we want to show that
    // the searchItemClient can check a user's authorization to access search items by itself.
    @Operation(summary = "List up to 20 transit decisions whose 'decided_by' starts with the given value (case-insensitive).",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping
    public List<SearchItemView> list(
            @RequestParam("decidedBy") String decidedBy) {
        Query prefix = Query.of(q -> q.prefix(p -> p
                .field(DECIDED_BY_FIELD)
                .value(decidedBy)
                .caseInsensitive(true)));

        return searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE, INDEX_TYPE_V2),
                prefix,
                b -> b.size(PAGE_SIZE)
        );
    }
}
