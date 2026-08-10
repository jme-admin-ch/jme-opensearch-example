package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.domain.SearchItemView;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClient;
import ch.admin.bit.jeap.opensearch.indextype.IndexType;
import ch.admin.bit.jme.opensearch.index.jme.decreedocument.JmeDecreeDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.decreedocument.JmeDecreeDocumentIndexTypeV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/decreedocuments")
@Tag(name = "DecreeDocuments", description = "Inspect indexed decree documents from the Process Archive example.")
@RequiredArgsConstructor
public class DecreeDocumentController {

    private static final IndexType<JmeDecreeDocumentDataV1> INDEX_TYPE = JmeDecreeDocumentIndexTypeV1.INSTANCE;
    private final SearchItemClient searchItemClient;

    @Operation(summary = "Find a decree document by origin ID.", security = @SecurityRequirement(name = "OIDC"))
    @GetMapping
    public ResponseEntity<SearchItemView> find(@RequestParam("originId") String originId) {
        Query prefix = Query.of(q -> q.prefix(p -> p.field("origin.id").value(originId).caseInsensitive(true)));
        List<SearchItemView> result = searchItemClient.searchMultiVersionWithUserAuth(
                List.of(INDEX_TYPE), prefix, request -> request.size(20));
        return ResponseEntity.of(result.stream().findFirst());
    }
}
