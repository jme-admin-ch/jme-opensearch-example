package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetAcceptance;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetNameInvalidException;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetNotAllowedException;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetNotConcreteException;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetNotFoundException;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.PolicyCheckOutcome;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Starts all four services against real Kafka and OpenSearch infrastructure, publishes the example messages through
 * the resource API and verifies the indexed documents through the authorization-aware inspection API.
 */
class OpenSearchExampleIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8180/jme-opensearch-auth-scs";
    private static final String RESOURCE_BASE_URL = "http://localhost:8581/jme-opensearch-resource-service";
    private static final String INDEX_WRITER_BASE_URL = "http://localhost:8580/jme-opensearch-index-writer-service";
    private static final String INSPECTION_BASE_URL = "http://localhost:8582/jme-opensearch-inspection-service";
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private static String accessToken;

    @Value("${jme.opensearch.url}")
    private String openSearchUrl;

    @BeforeAll
    static void startServices() throws Exception {
        startService("jme-opensearch-auth-scs", AUTH_BASE_URL);
        startService("jme-opensearch-resource-service", RESOURCE_BASE_URL);
        startService("jme-opensearch-index-writer-service", INDEX_WRITER_BASE_URL);
        startService("jme-opensearch-inspection-service", INSPECTION_BASE_URL);

        KafkaConsumerGroupAwaiter.waitForAssignment(
                "jme-opensearch-index-writer-service",
                "jme-create-transit-document",
                "jme-transit-decision-created");

        accessToken = given()
                .baseUri(AUTH_BASE_URL)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", "inspection-internal-sys")
                .formParam("client_secret", "secret")
                .when()
                .post("/oauth2/token")
                .then()
                .statusCode(200)
                .extract().path("access_token");
        assertThat(accessToken).isNotBlank();
    }

    @Test
    void createsAndIndexesTransitDocument() {
        Response created = given()
                .baseUri(RESOURCE_BASE_URL)
                .when()
                .post("/api/transitdocuments")
                .then()
                .statusCode(201)
                .extract().response();

        String originId = created.path("searchItem.origin.id");
        String goodsDescription = created.path("searchItem.data.goods_description");
        List<String> keywords = created.jsonPath().getList("searchItem.data.keywords", String.class);
        List<?> customsChecks = created.jsonPath().getList("searchItem.data.customs_checks");
        String customsOffice = created.path("searchItem.data.customs_checks[0].office");
        List<String> customsTags = created.jsonPath()
                .getList("searchItem.data.customs_checks[0].tags", String.class);
        List<String> customsCodes = created.jsonPath()
                .getList("searchItem.data.customs_checks[0].details.codes", String.class);
        assertThat(originId).isNotBlank();
        assertThat(goodsDescription).isNotBlank();
        assertThat(keywords).isNotEmpty();
        assertThat(customsChecks).hasSize(2);
        assertThat(customsOffice).isNotBlank();
        assertThat(customsTags).isNotEmpty();
        assertThat(customsCodes).isNotEmpty();

        String firstToken = goodsDescription.split(" ")[0];
        await().atMost(TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Response inspection = given()
                    .baseUri(INSPECTION_BASE_URL)
                    .auth().oauth2(accessToken)
                    .queryParam("goodsDescription", firstToken)
                    .when()
                    .get("/api/transitdocuments");

            assertThat(inspection.statusCode()).isEqualTo(200);
            String createdDocument = "find { it.origin.id == '%s' }".formatted(originId);
            assertThat(inspection.jsonPath().getMap(createdDocument)).isNotNull();
            assertThat(inspection.jsonPath().getList(createdDocument + ".data.keywords", String.class))
                    .isEqualTo(keywords);
            assertThat(inspection.jsonPath().getList(createdDocument + ".data.customs_checks"))
                    .isEqualTo(customsChecks);
        });

        assertTransitDocumentSearch("/api/transitdocuments/by-keyword", "keyword", keywords.getFirst(), originId);
        assertTransitDocumentSearch("/api/transitdocuments/by-customs-office", "office", customsOffice, originId);
        assertTransitDocumentSearch("/api/transitdocuments/by-customs-tag", "tag", customsTags.getFirst(), originId);
        assertTransitDocumentSearch("/api/transitdocuments/by-customs-code", "code", customsCodes.getFirst(), originId);
    }

    @Test
    void createsAndIndexesTransitDecisionWithCustomAnalysis() {
        Response created = given()
                .baseUri(RESOURCE_BASE_URL)
                .when()
                .post("/api/v3/transitdescisions")
                .then()
                .statusCode(201)
                .extract().response();

        String originId = created.path("searchItem.origin.id");
        String decidedBy = created.path("searchItem.data.decided_by");
        String remarks = created.path("searchItem.data.remarks");
        assertThat(originId).isNotBlank();
        assertThat(created.jsonPath().getInt("indexMajorVersion")).isEqualTo(3);
        assertThat(decidedBy).isEqualTo("Peter Müller");
        assertThat(remarks).isEqualTo("Café & Müller-AG");

        await().atMost(TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Response inspection = given()
                    .baseUri(INSPECTION_BASE_URL)
                    .auth().oauth2(accessToken)
                    .queryParam("decidedBy", "peter muller")
                    .when()
                    .get("/api/transitdecisions");

            assertThat(inspection.statusCode()).isEqualTo(200);
            String createdDecision = "find { it.origin.id == '%s' }".formatted(originId);
            assertThat(inspection.jsonPath().getString(createdDecision + ".data.decided_by"))
                    .isEqualTo(decidedBy);
        });

        Response analyzedRemarks = given()
                .baseUri(INSPECTION_BASE_URL)
                .auth().oauth2(accessToken)
                .queryParam("remarks", "cafe muller")
                .when()
                .get("/api/transitdecisions/by-remarks");

        assertThat(analyzedRemarks.statusCode()).isEqualTo(200);
        String createdDecision = "find { it.origin.id == '%s' }".formatted(originId);
        assertThat(analyzedRemarks.jsonPath().getString(createdDecision + ".data.remarks"))
                .isEqualTo(remarks);
    }

    @Test
    void providesCurrentTransitDecisionInRequestedSchemaMajor() {
        String originId = given()
                .baseUri(RESOURCE_BASE_URL)
                .when()
                .post("/api/v3/transitdescisions")
                .then()
                .statusCode(201)
                .extract().path("searchItem.origin.id");
        String searchItemReadToken = clientCredentialsToken("index-writer-read-client");

        Response major1 = currentSearchItem(searchItemReadToken, originId, 1);
        assertThat(major1.statusCode()).isEqualTo(200);
        assertThat(major1.header("index-major-version")).isEqualTo("1");
        assertThat(major1.header("index-minor-version")).isEqualTo("0");
        assertThat(major1.jsonPath().getString("data.transit_decision_id")).isEqualTo(originId);

        Response major2 = currentSearchItem(searchItemReadToken, originId, 2);
        assertThat(major2.statusCode()).isEqualTo(200);
        assertThat(major2.header("index-major-version")).isEqualTo("2");
        assertThat(major2.jsonPath().getString("data.transit_decision_identifier")).isEqualTo(originId);
        assertThat(major2.jsonPath().getString("origin.version")).isEqualTo(major1.jsonPath().getString("origin.version"));

        Response unsupported = currentSearchItem(searchItemReadToken, originId, 4);
        assertThat(unsupported.statusCode()).isEqualTo(422);
        assertThat(unsupported.jsonPath().getString("code")).isEqualTo("UNSUPPORTED_INDEX_MAJOR_VERSION");

        assertThat(currentSearchItem(accessToken, originId, 1).statusCode()).isEqualTo(401);
    }

    @Test
    void checksAppliedPolicyOfMaintenanceTarget() {
        String rolling = "jme_policy_check_rolling-000001";
        String nonRolling = "jme_policy_check_non_rolling-000001";
        String unmanaged = "jme_policy_check_unmanaged-000001";
        IsmFixtures ism = new IsmFixtures(openSearchUrl);
        try (MaintenancePolicyCheck policyCheck = new MaintenancePolicyCheck(openSearchUrl)) {
            ism.ensureIsmConfigIndexOpen();
            List.of(rolling, nonRolling, unmanaged).forEach(ism::recreateIndex);
            ism.recreatePolicies();
            ism.attachPolicy(rolling, IsmFixtures.ROLLOVER_POLICY_ID);
            ism.attachPolicy(nonRolling, IsmFixtures.NO_ROLLOVER_POLICY_ID);
            ism.awaitPoliciesApplied(policyCheck, Map.of(
                    rolling, PolicyCheckOutcome.ROLLOVER_IN_POLICY,
                    nonRolling, PolicyCheckOutcome.NO_ROLLOVER_IN_POLICY));

            JobTargetAcceptance refused = policyCheck.check(rolling);
            assertThat(refused.allowed()).isFalse();
            assertThat(refused.reason()).contains(rolling, IsmFixtures.ROLLOVER_POLICY_ID, "[warm]");
            assertThatThrownBy(refused::requireAllowed)
                    .isInstanceOfSatisfying(JobTargetNotAllowedException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo("not-allowed"));

            assertThat(policyCheck.check(nonRolling).allowed()).isTrue();
            JobTargetAcceptance noPolicy = policyCheck.check(unmanaged);
            assertThat(noPolicy.outcome()).isEqualTo(PolicyCheckOutcome.NO_POLICY_APPLIED);
            assertThat(noPolicy.allowed()).isTrue();
            assertThat(noPolicy.reason()).contains(unmanaged, "No ISM policy is reported as applied");

            ism.withUnreadablePolicyMetadata(() -> {
                JobTargetAcceptance undetermined = policyCheck.check(rolling);
                assertThat(undetermined.outcome()).isEqualTo(PolicyCheckOutcome.POLICY_UNDETERMINED);
                assertThat(undetermined.allowed()).isTrue();
                assertThat(undetermined.reason()).contains("could not be determined", "without verifying non-rolling operation");
            });
        } finally {
            List.of(rolling, nonRolling, unmanaged).forEach(ism::deleteIndex);
            ism.deletePolicies();
        }
    }

    @Test
    void refusesMaintenanceTargetsThatAreNotAnExistingPhysicalIndex() {
        try (MaintenancePolicyCheck policyCheck = new MaintenancePolicyCheck(openSearchUrl)) {
            assertThatThrownBy(() -> policyCheck.check("jme_transit_decision_v1_write"))
                    .isInstanceOfSatisfying(JobTargetNotConcreteException.class, e -> {
                        assertThat(e).hasMessageContaining("not a concrete index");
                        assertThat(e.getResolvedIndices()).contains("jme_transit_decision_v1-000001");
                    });
            assertThatThrownBy(() -> policyCheck.check("jme_policy_check_missing-000001"))
                    .isInstanceOf(JobTargetNotFoundException.class).hasMessageContaining("does not exist");
            assertThatThrownBy(() -> policyCheck.check("jme_*"))
                    .isInstanceOf(JobTargetNameInvalidException.class);
        }
    }

    private static Response currentSearchItem(String token, String originId, int indexMajorVersion) {
        return given()
                .baseUri(RESOURCE_BASE_URL)
                .auth().oauth2(token)
                .queryParam("index_type", "JmeTransitDecision")
                .queryParam("origin_id", originId)
                .queryParam("index_major_version", indexMajorVersion)
                .when()
                .get("/index-api/searchitems");
    }

    private static String clientCredentialsToken(String clientId) {
        return given()
                .baseUri(AUTH_BASE_URL)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", "secret")
                .when()
                .post("/oauth2/token")
                .then()
                .statusCode(200)
                .extract().path("access_token");
    }

    private static void assertTransitDocumentSearch(String path, String parameter, String value, String originId) {
        Response response = given()
                .baseUri(INSPECTION_BASE_URL)
                .auth().oauth2(accessToken)
                .queryParam(parameter, value)
                .when()
                .get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("origin.id", String.class)).contains(originId);
    }
}
