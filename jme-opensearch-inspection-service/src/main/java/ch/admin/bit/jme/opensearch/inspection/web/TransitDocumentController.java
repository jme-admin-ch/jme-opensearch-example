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
    private static final String KEYWORDS_FIELD = "data.keywords";
    private static final String CUSTOMS_CHECKS_PATH = "data.customs_checks";
    private static final String CUSTOMS_OFFICE_FIELD = CUSTOMS_CHECKS_PATH + ".office";
    private static final String CUSTOMS_TAGS_FIELD = CUSTOMS_CHECKS_PATH + ".tags";
    private static final String CUSTOMS_CODES_FIELD = CUSTOMS_CHECKS_PATH + ".details.codes";

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

        return search(prefix);
    }

    @Operation(summary = "Find transit documents containing the given keyword.",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping("/by-keyword")
    public List<SearchItemView> byKeyword(@RequestParam String keyword) {
        return search(term(KEYWORDS_FIELD, keyword));
    }

    @Operation(summary = "Find transit documents with a customs check for the given office.",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping("/by-customs-office")
    public List<SearchItemView> byCustomsOffice(@RequestParam String office) {
        return search(nestedTerm(CUSTOMS_OFFICE_FIELD, office));
    }

    @Operation(summary = "Find transit documents with a customs check containing the given tag.",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping("/by-customs-tag")
    public List<SearchItemView> byCustomsTag(@RequestParam String tag) {
        return search(nestedTerm(CUSTOMS_TAGS_FIELD, tag));
    }

    @Operation(summary = "Find transit documents with a customs-check detail containing the given code.",
            security = {@SecurityRequirement(name = "OIDC")})
    @GetMapping("/by-customs-code")
    public List<SearchItemView> byCustomsCode(@RequestParam String code) {
        return search(nestedTerm(CUSTOMS_CODES_FIELD, code));
    }

    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t
                .field(field)
                .value(v -> v.stringValue(value))));
    }

    private Query nestedTerm(String field, String value) {
        return Query.of(q -> q.nested(n -> n
                .path(CUSTOMS_CHECKS_PATH)
                .query(term(field, value))));
    }

    private List<SearchItemView> search(Query query) {
        return searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE),
                query,
                b -> b.size(PAGE_SIZE)
        );
    }
}
