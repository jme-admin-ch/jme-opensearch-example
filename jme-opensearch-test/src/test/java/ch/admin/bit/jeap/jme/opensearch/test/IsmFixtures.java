package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.PolicyCheckOutcome;
import io.restassured.specification.RequestSpecification;

import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

/**
 * Plain REST helpers to manage the indices and ISM policies of the maintenance policy check tests. They do not need
 * a Spring context, so cleanup also works when creating the {@link MaintenancePolicyCheck} failed.
 */
final class IsmFixtures {

    static final String ROLLOVER_POLICY_ID = "jme-policy-check-rollover";
    static final String NO_ROLLOVER_POLICY_ID = "jme-policy-check-no-rollover";
    private static final String ISM_CONFIG_INDEX = ".opendistro-ism-config";
    // The rollover action sits in a later state: the check must find it although the index never reaches that state
    private static final String ROLLOVER_POLICY = """
            {"policy":{"description":"Demo: rolls the index over in its warm state","default_state":"hot","states":[
              {"name":"hot","actions":[],"transitions":[{"state_name":"warm","conditions":{"min_index_age":"3650d"}}]},
              {"name":"warm","actions":[{"rollover":{"min_doc_count":100000000}}],"transitions":[]}]}}
            """;
    private static final String NO_ROLLOVER_POLICY = """
            {"policy":{"description":"Demo: manages replicas, never rolls over","default_state":"hot","states":[
              {"name":"hot","actions":[{"replica_count":{"number_of_replicas":0}}],"transitions":[]}]}}
            """;
    private static final Duration ISM_INITIALIZATION_TIMEOUT = Duration.ofMinutes(5);

    private final String openSearchUrl;

    IsmFixtures(String openSearchUrl) {
        this.openSearchUrl = openSearchUrl;
    }

    // A previous run killed while the index was closed would otherwise leave ISM broken for every following run
    // On a fresh cluster the index does not exist before the first policy is created, then there is nothing to reopen
    void ensureIsmConfigIndexOpen() {
        int status = openSearch().queryParam("wait_for_active_shards", "1").post("/" + ISM_CONFIG_INDEX + "/_open")
                .then().extract().statusCode();
        if (status != 404) {
            reopenIsmConfigIndex();
        }
    }

    void recreatePolicies() {
        deletePolicies();
        openSearch().body(ROLLOVER_POLICY).put("/_plugins/_ism/policies/" + ROLLOVER_POLICY_ID).then().statusCode(201);
        openSearch().body(NO_ROLLOVER_POLICY).put("/_plugins/_ism/policies/" + NO_ROLLOVER_POLICY_ID).then().statusCode(201);
    }

    void deletePolicies() {
        for (String policyId : new String[]{ROLLOVER_POLICY_ID, NO_ROLLOVER_POLICY_ID}) {
            openSearch().delete("/_plugins/_ism/policies/" + policyId);
        }
    }

    void recreateIndex(String index) {
        deleteIndex(index);
        openSearch().body("{\"settings\":{\"number_of_replicas\":0}}").put("/" + index).then().statusCode(200);
    }

    void deleteIndex(String index) {
        openSearch().delete("/" + index);
    }

    // _ism/add answers 200 even when it cannot attach the policy, the failure is only reported in the body
    void attachPolicy(String index, String policyId) {
        openSearch().body("{\"policy_id\":\"%s\"}".formatted(policyId)).post("/_plugins/_ism/add/" + index)
                .then().statusCode(200)
                .body("updated_indices", is(1))
                .body("failures", is(false));
    }

    // ISM attaches a policy on its next job run; until then explain reports no policy and the check allows the index
    void awaitPoliciesApplied(MaintenancePolicyCheck policyCheck, Map<String, PolicyCheckOutcome> expectedByIndex) {
        await().atMost(ISM_INITIALIZATION_TIMEOUT).pollInterval(Duration.ofSeconds(2))
                .until(() -> expectedByIndex.entrySet().stream()
                        .allMatch(e -> policyCheck.check(e.getKey()).outcome() == e.getValue()));
    }

    // Explain reads applied policies from the ISM config index, so while it is closed no policy can be read
    void withUnreadablePolicyMetadata(Runnable action) {
        openSearch().post("/" + ISM_CONFIG_INDEX + "/_close").then().statusCode(200);
        try {
            action.run();
        } finally {
            reopenIsmConfigIndex();
        }
    }

    // The single node cannot assign replicas, so only the primary shard can be awaited
    private void reopenIsmConfigIndex() {
        openSearch().queryParam("wait_for_active_shards", "1").post("/" + ISM_CONFIG_INDEX + "/_open")
                .then().statusCode(200)
                .body("acknowledged", is(true))
                .body("shards_acknowledged", is(true));
    }

    private RequestSpecification openSearch() {
        return given().baseUri(openSearchUrl).contentType("application/json");
    }
}
