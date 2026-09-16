package ch.admin.bit.jeap.opensearch.resource.web;

import ch.admin.bit.jeap.opensearch.resource.domain.DomainService;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/transitdescisions")
@Tag(name = "TransitDecisions V3", description = "Create transit decision V3 with custom OpenSearch analysis.")
@RequiredArgsConstructor
public class TransitDecisionV3Controller {

    private final DomainService domainService;

    @Operation(
            summary = "Create a transit decision demonstrating accent folding and custom text analysis.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created")
            }
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchItemContainer> createTransitDecision() {
        SearchItemContainer searchItemContainer = domainService.createAndPublishTransitDecisionV3();

        return ResponseEntity
                .status(201)
                .body(searchItemContainer);
    }
}
