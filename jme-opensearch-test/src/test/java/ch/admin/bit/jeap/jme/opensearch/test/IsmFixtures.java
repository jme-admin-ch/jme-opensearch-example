package ch.admin.bit.jeap.jme.opensearch.test;

import io.restassured.specification.RequestSpecification;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

/**
 * Plain REST helpers to manage the indices and ISM policies used to verify that the maintenance job API refuses a
 * target with a rollover policy. No Spring context is needed, so cleanup also works when a setup step failed.
 */
final class IsmFixtures {

    static final String ROLLOVER_POLICY_ID = "jme-policy-check-rollover";
    private static final String ISM_CONFIG_INDEX = ".opendistro-ism-config";
    // The rollover action sits in a later state: the check must find it although the index never reaches that state
    private static final String ROLLOVER_POLICY = """
            {"policy":{"description":"Demo: rolls the index over in its warm state","default_state":"hot","states":[
              {"name":"hot","actions":[],"transitions":[{"state_name":"warm","conditions":{"min_index_age":"3650d"}}]},
              {"name":"warm","actions":[{"rollover":{"min_doc_count":100000000}}],"transitions":[]}]}}
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
    }

    void deletePolicies() {
        openSearch().delete("/_plugins/_ism/policies/" + ROLLOVER_POLICY_ID);
    }

    // _ism/add answers 200 even when it cannot attach the policy, the failure is only reported in the body
    void attachPolicy(String index, String policyId) {
        openSearch().body("{\"policy_id\":\"%s\"}".formatted(policyId)).post("/_plugins/_ism/add/" + index)
                .then().statusCode(200)
                .body("updated_indices", is(1))
                .body("failures", is(false));
    }

    void detachPolicy(String index) {
        openSearch().post("/_plugins/_ism/remove/" + index).then().statusCode(200);
    }

    // ISM attaches a policy on its next job run; until then explain reports no policy and the job API would accept the index
    void awaitPolicyAttached(String index, String policyId) {
        await().atMost(ISM_INITIALIZATION_TIMEOUT).pollInterval(Duration.ofSeconds(2))
                .until(() -> explain(index).contains("\"policy_id\":\"" + policyId + "\""));
    }

    void awaitPolicyDetached(String index) {
        await().atMost(ISM_INITIALIZATION_TIMEOUT).pollInterval(Duration.ofSeconds(2))
                .until(() -> explain(index).contains("\"index.plugins.index_state_management.policy_id\":null"));
    }

    private String explain(String index) {
        return openSearch().get("/_plugins/_ism/explain/" + index).asString();
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
