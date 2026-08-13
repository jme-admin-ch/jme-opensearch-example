package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jeap.opensearch.indextype.Origin;
import ch.admin.bit.jeap.opensearch.indextype.SearchItem;
import ch.admin.bit.jeap.opensearch.searchitem.api.SearchItemsProvider;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV2;
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
    private static final Random RANDOM = new Random();

    private final DomainRepository domainRepository;
    private final MessagePublisher messagePublisher;

    @Override
    public Optional<SearchItemContainer> findSearchItem(String indexType, String originId, String originVersion) {
        return domainRepository.findSearchItem(indexType, originId, originVersion);
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
        JmeTransitDecisionDataV1 transitDecision = createTransitDecisionDto();
        Origin searchItemOrigin = createOrigin(transitDecision.transitDecisionId());

        SearchItem<JmeTransitDecisionDataV1> searchItem = new SearchItem<>(searchItemOrigin, transitDecision);

        SearchItemContainer searchItemContainer = new SearchItemContainer(
                1,
                JmeTransitDecisionIndexTypeV1.INSTANCE.minorVersion(),
                searchItem
        );

        domainRepository.saveSearchItemContainer("JmeTransitDecision", searchItemOrigin.id(), null, searchItemContainer);

        messagePublisher.transitDecisionCreated(transitDecision);

        return searchItemContainer;
    }

    public SearchItemContainer createAndPublishTransitDecisionV2() {
        JmeTransitDecisionDataV2 transitDecision = createTransitDecisionDtoV2();
        Origin searchItemOrigin = createOrigin(transitDecision.transitDecisionIdentifier());

        SearchItem<JmeTransitDecisionDataV2> searchItem = new SearchItem<>(searchItemOrigin, transitDecision);

        SearchItemContainer searchItemContainer = new SearchItemContainer(
                2,
                JmeTransitDecisionIndexTypeV2.INSTANCE.minorVersion(),
                searchItem
        );

        domainRepository.saveSearchItemContainer("JmeTransitDecision", searchItemOrigin.id(), null, searchItemContainer);

        messagePublisher.transitDecisionCreatedV2(transitDecision);

        return searchItemContainer;
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

    private JmeTransitDecisionDataV1 createTransitDecisionDto() {
        return new JmeTransitDecisionDataV1(
                UUID.randomUUID().toString(),
                randomFrom(List.of(JmeTransitDecisionStatus.values())).name(),
                randomFrom(REMARKS),
                randomFrom(OFFICERS),
                Instant.now().minus(1, ChronoUnit.DAYS),
                UUID.randomUUID().toString());
    }

    private JmeTransitDecisionDataV2 createTransitDecisionDtoV2() {
        return new JmeTransitDecisionDataV2(
                UUID.randomUUID().toString(),
                randomFrom(List.of(JmeTransitDecisionStatus.values())).name(),
                randomFrom(REMARKS),
                randomFrom(OFFICERS),
                Instant.now().minus(1, ChronoUnit.DAYS),
                UUID.randomUUID().toString());
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
