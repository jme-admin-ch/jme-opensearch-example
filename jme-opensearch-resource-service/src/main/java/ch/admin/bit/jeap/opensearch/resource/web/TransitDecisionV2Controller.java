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
@RequestMapping("/api/v2/transitdescisions")
@Tag(name = "TransitDecisions V2", description = "Create transit decision V2.")
@RequiredArgsConstructor
public class TransitDecisionV2Controller {

    private final DomainService domainService;

    @Operation(
            summary = "Create a new transit decision V2.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created")
            }
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchItemContainer> createTransitDecision() {
        SearchItemContainer searchItemContainer = domainService.createAndPublishTransitDecisionV2();

        return ResponseEntity
                .status(201)
                .body(searchItemContainer);
    }
}
