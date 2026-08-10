package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.domain.SearchItemView;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;
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
@RequestMapping("/api/transitdocuments")
@Tag(name = "TransitDocuments", description = "Inspect indexed transit documents.")
@RequiredArgsConstructor
public class TransitDocumentController {

    private static final int PAGE_SIZE = 20;
    private static final IndexType<JmeTransitDocumentDataV1> INDEX_TYPE =
            JmeTransitDocumentIndexTypeV1.INSTANCE;
    private static final String GOODS_DESCRIPTION_FIELD = "data.goods_description";

    private final SearchItemClient searchItemClient;

    // No pre-authorization here on the endpoint for this example as we want to show that
    // the searchItemClient can check a user's authorization to access search items by itself.
    @Operation(summary = "List up to 20 transit documents whose 'goods_description' token starts with the given value (case-insensitive).",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping
    public List<SearchItemView> list(
            @RequestParam("goodsDescription") String goodsDescription) {
        Query prefix = Query.of(q -> q.prefix(p -> p
                .field(GOODS_DESCRIPTION_FIELD)
                .value(goodsDescription)
                .caseInsensitive(true)));

        return searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE),
                prefix,
                b -> b.size(PAGE_SIZE)
        );
    }
}
