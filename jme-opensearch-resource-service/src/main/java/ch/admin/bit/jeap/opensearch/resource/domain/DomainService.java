package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jeap.opensearch.indextype.Origin;
import ch.admin.bit.jeap.opensearch.indextype.SearchItem;
import ch.admin.bit.jeap.opensearch.searchitem.api.SearchItemsProvider;
import ch.admin.bit.jeap.opensearch.searchitem.api.exception.SearchItemsBadInputException;
import ch.admin.bit.jeap.opensearch.searchitem.api.exception.UnsupportedIndexMajorVersionException;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV3;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV3;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;
import ch.admin.bit.jme.transit.JmeTransitDecisionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DomainService implements SearchItemsProvider {

    private static final List<String> OFFICES = List.of(
            "Basel", "Zurich", "Geneva", "Bern", "Lugano", "Chiasso", "St. Gallen"
    );
    private static final List<String> GOODS = List.of(
            "Electronics", "Textiles", "Machinery", "Food products", "Chemicals", "Furniture", "Vehicles"
    );
    private static final List<String> REMARKS = List.of(
            "Urgent shipment", "Handle with care", "Fragile contents", "Temperature sensitive", "No special handling required"
    );
    private static final List<String> FREETEXT = List.of(
            "Hallo", "Hello", "Gugu", "Bonjour", "Buongiorno"
    );
    private static final List<String> OFFICERS = List.of(
            "John Doe", "Jane Smith", "Peter Müller", "Anna Keller", "Marco Rossi", "John Rossi"
    );
    private static final List<String> BUSINESS_PARTNERS = List.of(
            "BP1", "BP2", "BP3"
    );
    private static final List<String> TENANTS = List.of(
            "TENANT1", "TENANT2", "TENANT3"
    );
    private static final List<String> URLS = List.of(
            "https://github.com/jme-admin-ch/jme-opensearch-example", "https://www.google.com/", "https://hello.ch"
    );
    static final String TRANSIT_DECISION_INDEX_TYPE = "JmeTransitDecision";
    private static final Random RANDOM = new Random();

    private final DomainRepository domainRepository;
    private final MessagePublisher messagePublisher;

    @Override
    public Optional<SearchItemContainer> findSearchItem(String indexType, String originId, String originVersion) {
        return domainRepository.findSearchItem(indexType, originId, originVersion);
    }

    /**
     * Loads the current transit decision and builds the representation for the requested schema major. The same
     * object is available in majors 1, 2 and 3; any other major is reported as unsupported instead of falling back
     * to another one. Other index types keep the default behaviour of {@link SearchItemsProvider}.
     */
    @Override
    public Optional<SearchItemContainer> findCurrentSearchItem(String indexType, String originId, int indexMajorVersion)
            throws SearchItemsBadInputException, UnsupportedIndexMajorVersionException {
        if (!TRANSIT_DECISION_INDEX_TYPE.equals(indexType)) {
            return SearchItemsProvider.super.findCurrentSearchItem(indexType, originId, indexMajorVersion);
        }
        Function<TransitDecision, SearchItemContainer> projection = switch (indexMajorVersion) {
            case 1 -> DomainService::transitDecisionV1;
            case 2 -> DomainService::transitDecisionV2;
            case 3 -> DomainService::transitDecisionV3;
            default -> throw new UnsupportedIndexMajorVersionException(indexType, indexMajorVersion);
        };
        return domainRepository.findCurrentTransitDecision(originId).map(projection);
    }

    public SearchItemContainer createAndPublishCreateTransitDocument() {
        JmeTransitDocumentDataV1 transitDocument = createTransitDocument();
        Origin searchItemOrigin = createOrigin(transitDocument.transitDocumentId());

        SearchItem<JmeTransitDocumentDataV1> searchItem = new SearchItem<>(searchItemOrigin, transitDocument);

        SearchItemContainer searchItemContainer = new SearchItemContainer(
                1,
                JmeTransitDocumentIndexTypeV1.INSTANCE.minorVersion(),
                searchItem
        );

        domainRepository.saveSearchItemContainer("JmeTransitDocument", searchItemOrigin.id(), null, searchItemContainer);

        messagePublisher.createTransitDocument(transitDocument);

        return searchItemContainer;
    }

    public SearchItemContainer createAndPublishTransitDecision() {
        TransitDecision transitDecision = saveTransitDecision(randomFrom(REMARKS), randomFrom(OFFICERS));
        SearchItemContainer searchItemContainer = saveLiveSearchItem(transitDecisionV1(transitDecision));
        messagePublisher.transitDecisionCreated(toDataV1(transitDecision));
        return searchItemContainer;
    }

    public SearchItemContainer createAndPublishTransitDecisionV2() {
        TransitDecision transitDecision = saveTransitDecision(randomFrom(REMARKS), randomFrom(OFFICERS));
        SearchItemContainer searchItemContainer = saveLiveSearchItem(transitDecisionV2(transitDecision));
        messagePublisher.transitDecisionCreatedV2(toDataV2(transitDecision));
        return searchItemContainer;
    }

    public SearchItemContainer createAndPublishTransitDecisionV3() {
        TransitDecision transitDecision = saveTransitDecision("Café & Müller-AG", "Peter Müller");
        SearchItemContainer searchItemContainer = saveLiveSearchItem(transitDecisionV3(transitDecision));
        messagePublisher.transitDecisionCreatedV3(toDataV3(transitDecision));
        return searchItemContainer;
    }

    private TransitDecision saveTransitDecision(String remarks, String decidedBy) {
        TransitDecision transitDecision = new TransitDecision(
                createOrigin(UUID.randomUUID().toString()),
                randomFrom(List.of(JmeTransitDecisionStatus.values())).name(),
                remarks,
                decidedBy,
                Instant.now().minus(1, ChronoUnit.DAYS),
                UUID.randomUUID().toString());
        domainRepository.saveCurrentTransitDecision(transitDecision);
        return transitDecision;
    }

    private SearchItemContainer saveLiveSearchItem(SearchItemContainer searchItemContainer) {
        domainRepository.saveSearchItemContainer(TRANSIT_DECISION_INDEX_TYPE,
                searchItemContainer.searchItem().origin().id(), null, searchItemContainer);
        return searchItemContainer;
    }

    private static SearchItemContainer transitDecisionV1(TransitDecision transitDecision) {
        return new SearchItemContainer(1, JmeTransitDecisionIndexTypeV1.INSTANCE.minorVersion(),
                new SearchItem<>(transitDecision.origin(), toDataV1(transitDecision)));
    }

    private static SearchItemContainer transitDecisionV2(TransitDecision transitDecision) {
        return new SearchItemContainer(2, JmeTransitDecisionIndexTypeV2.INSTANCE.minorVersion(),
                new SearchItem<>(transitDecision.origin(), toDataV2(transitDecision)));
    }

    private static SearchItemContainer transitDecisionV3(TransitDecision transitDecision) {
        return new SearchItemContainer(3, JmeTransitDecisionIndexTypeV3.INSTANCE.minorVersion(),
                new SearchItem<>(transitDecision.origin(), toDataV3(transitDecision)));
    }

    private static JmeTransitDecisionDataV1 toDataV1(TransitDecision d) {
        return new JmeTransitDecisionDataV1(d.id(), d.status(), d.remarks(), d.decidedBy(), d.decisionDate(), d.transitDocumentId());
    }

    private static JmeTransitDecisionDataV2 toDataV2(TransitDecision d) {
        return new JmeTransitDecisionDataV2(d.id(), d.status(), d.remarks(), d.decidedBy(), d.decisionDate(), d.transitDocumentId());
    }

    private static JmeTransitDecisionDataV3 toDataV3(TransitDecision d) {
        return new JmeTransitDecisionDataV3(d.id(), d.status(), d.remarks(), d.decidedBy(), d.decisionDate(), d.transitDocumentId());
    }

    private JmeTransitDocumentDataV1 createTransitDocument() {
        return new JmeTransitDocumentDataV1(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                randomFrom(OFFICES),
                randomFrom(OFFICES),
                randomFrom(GOODS),
                randomFrom(REMARKS),
                "Text document",
                randomFrom(FREETEXT),
                List.of("transit", "customs"),
                List.of(
                        new JmeTransitDocumentDataV1.CustomsChecks(
                                "Basel",
                                List.of("inspection", "cleared"),
                                new JmeTransitDocumentDataV1.CustomsChecks.Details(List.of("A1", "B2"))),
                        new JmeTransitDocumentDataV1.CustomsChecks(
                                "Zurich",
                                List.of("documentary"),
                                new JmeTransitDocumentDataV1.CustomsChecks.Details(List.of("C3")))));
    }

    private static Map<String, String> createUrl() {
        return Map.of("url", randomFrom(URLS));
    }

    private static Origin createOrigin(String id) {
        return new Origin(id,
                "1.0.0",
                randomFromOrNull(BUSINESS_PARTNERS),
                randomFromOrNull(TENANTS),
                Instant.now(),
                Instant.now(),
                createUrl());
    }

    private static <T> T randomFromOrNull(List<T> list) {
        if (RANDOM.nextBoolean()) {
            return null;
        }
        return randomFrom(list);
    }

    private static <T> T randomFrom(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

}
