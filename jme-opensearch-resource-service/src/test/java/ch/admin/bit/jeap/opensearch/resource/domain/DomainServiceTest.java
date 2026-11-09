package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jeap.opensearch.searchitem.api.exception.UnsupportedIndexMajorVersionException;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV3;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DomainServiceTest {

    private static final String TRANSIT_DECISION = DomainService.TRANSIT_DECISION_INDEX_TYPE;

    private final DomainService domainService = new DomainService(new DomainRepository(), mock(MessagePublisher.class));

    @Test
    void returnsSameCurrentObjectInMajors1And2() throws Exception {
        String id = domainService.createAndPublishTransitDecisionV3().searchItem().origin().id();

        SearchItemContainer major1 = domainService.findCurrentSearchItem(TRANSIT_DECISION, id, 1).orElseThrow();
        SearchItemContainer major2 = domainService.findCurrentSearchItem(TRANSIT_DECISION, id, 2).orElseThrow();

        assertThat(major1.indexMajorVersion()).isEqualTo(1);
        assertThat(major2.indexMajorVersion()).isEqualTo(2);
        assertThat(major1.searchItem().origin()).isEqualTo(major2.searchItem().origin());

        JmeTransitDecisionDataV1 v1 = (JmeTransitDecisionDataV1) major1.searchItem().data();
        JmeTransitDecisionDataV2 v2 = (JmeTransitDecisionDataV2) major2.searchItem().data();
        assertThat(v1.transitDecisionId()).isEqualTo(id);
        assertThat(v2.transitDecisionIdentifier()).isEqualTo(id);
        assertThat(v1.remarks()).isEqualTo(v2.remarks()).isEqualTo("Café & Müller-AG");
        assertThat(v1.decidedBy()).isEqualTo(v2.decidedBy());
        assertThat(v1.decisionDate()).isEqualTo(v2.decisionDate());
    }

    @Test
    void majorIsIndependentOfTheMajorThatTriggeredTheLiveEvent() throws Exception {
        String id = domainService.createAndPublishTransitDecision().searchItem().origin().id();

        SearchItemContainer major3 = domainService.findCurrentSearchItem(TRANSIT_DECISION, id, 3).orElseThrow();

        assertThat(major3.indexMajorVersion()).isEqualTo(3);
        assertThat(major3.searchItem().data()).isInstanceOf(JmeTransitDecisionDataV3.class);
    }

    @Test
    void unsupportedMajorIsAClearErrorNotAnotherMajor() {
        String id = domainService.createAndPublishTransitDecisionV3().searchItem().origin().id();

        assertThatThrownBy(() -> domainService.findCurrentSearchItem(TRANSIT_DECISION, id, 4))
                .isInstanceOf(UnsupportedIndexMajorVersionException.class)
                .hasMessageContaining("JmeTransitDecision")
                .hasMessageContaining("4");
    }

    @Test
    void unknownObjectIsNotFound() throws Exception {
        assertThat(domainService.findCurrentSearchItem(TRANSIT_DECISION, "unknown", 1)).isEmpty();
    }

    @Test
    void callerWithoutSchemaMajorKeepsTodaysBehaviour() {
        SearchItemContainer created = domainService.createAndPublishTransitDecisionV2();
        String id = created.searchItem().origin().id();

        assertThat(domainService.findSearchItem(TRANSIT_DECISION, id, null)).contains(created);
    }

    @Test
    void otherIndexTypesUseTheDefaultUpgradePath() throws Exception {
        SearchItemContainer created = domainService.createAndPublishCreateTransitDocument();
        String id = created.searchItem().origin().id();

        assertThat(domainService.findCurrentSearchItem("JmeTransitDocument", id, 1)).contains(created);
        assertThatThrownBy(() -> domainService.findCurrentSearchItem("JmeTransitDocument", id, 2))
                .isInstanceOf(UnsupportedIndexMajorVersionException.class);
    }
}
