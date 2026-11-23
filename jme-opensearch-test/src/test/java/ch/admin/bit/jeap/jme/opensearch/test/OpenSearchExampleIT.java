package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
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
    private static final String YAML = "application/yaml";
    private static final String JOB_INDEX = "jeap_indexwriter_jobs";
    private static final String TASK_INDEX = "jeap_indexwriter_tasks";

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

        assertNoIsmPolicyApplied("jme_transit_document_v1-000001");
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
    void submitsAndRetrievesUpsertJobsInYaml() {
        String operator = clientCredentialsToken("job-operator-sys");

        String explicitId = UUID.randomUUID().toString();
        Map<String, Object> explicit = putYaml(operator, "/api/upsert-jobs/" + explicitId, upsertJob("indexMajorVersion: 2", "3"), 200);
        assertThat(explicit).containsEntry("job-id", explicitId)
                .containsEntry("job-type", "upsert")
                .containsEntry("job-state", "open");
        assertThat(target(explicit)).containsEntry("selection", "explicit").containsEntry("index-major-version", 2)
                .containsEntry("physical-index", "jme_transit_decision_v2-000001");
        assertThat(references(explicit)).extracting(r -> r.get("id")).containsExactly("jme-ref-1", "jme-ref-2");
        assertThat(references(explicit).getFirst()).containsEntry("version", "3").containsEntry("state", "open");
        assertThat(references(explicit).get(1)).doesNotContainKey("version");
        assertThat(explicit).doesNotContainKeys("actual-index-major-version", "job-result");

        // The omitted major selects the highest configured major (3); the stored version "1" does not select a schema
        String omittedId = UUID.randomUUID().toString();
        Map<String, Object> omitted = putYaml(operator, "/api/upsert-jobs/" + omittedId, upsertJob("", "1"), 200);
        assertThat(target(omitted)).containsEntry("selection", "highest").containsEntry("index-major-version", 3)
                .containsEntry("physical-index", "jme_transit_decision_v3-000001");
        assertThat(references(omitted).getFirst()).containsEntry("version", "1");

        Map<String, Object> retrieved = getYaml(operator, "/api/upsert-jobs/" + explicitId, 200);
        assertThat(retrieved).containsEntry("job-id", explicitId);
        assertThat(target(retrieved)).isEqualTo(target(explicit));
        assertThat(references(retrieved)).isEqualTo(references(explicit));

        // Same content under the same id returns the retained job; a changed schema selector or stored version conflicts
        Map<String, Object> repeated = putYaml(operator, "/api/upsert-jobs/" + explicitId, upsertJob("indexMajorVersion: 2", "3"), 200);
        assertThat(repeated)
                .containsEntry("started", explicit.get("started"))
                .containsEntry("job-id", explicitId);
        putRaw(operator, "/api/upsert-jobs/" + explicitId, upsertJob("indexMajorVersion: 2", "4")).then().statusCode(409);
        putRaw(operator, "/api/upsert-jobs/" + explicitId, upsertJob("indexMajorVersion: 1", "3")).then().statusCode(409);
    }

    @Test
    void submitsAndRetrievesDeleteJobsInYaml() {
        String operator = clientCredentialsToken("job-operator-sys");

        String explicitId = UUID.randomUUID().toString();
        Map<String, Object> explicit = putYaml(operator, "/api/delete-jobs/" + explicitId, deleteJob("indexMajorVersion: 1"), 200);
        assertThat(explicit).containsEntry("job-type", "delete");
        assertThat(target(explicit)).containsEntry("selection", "explicit").containsEntry("index-major-version", 1)
                .containsEntry("physical-index", "jme_transit_decision_v1-000001");
        assertThat(references(explicit)).extracting(r -> r.get("id")).containsExactly("jme-ref-1", "jme-ref-2");
        assertThat(references(explicit)).allSatisfy(r -> assertThat(r).doesNotContainKeys("version", "actual-index-major-version"));

        Map<String, Object> omitted = putYaml(operator, "/api/delete-jobs/" + UUID.randomUUID(), deleteJob(""), 200);
        assertThat(target(omitted)).containsEntry("selection", "highest").containsEntry("index-major-version", 3);

        assertThat(getYaml(operator, "/api/delete-jobs/" + explicitId, 200)).containsEntry("job-id", explicitId);

        String withVersion = deleteJob("indexMajorVersion: 1").replace("- id: 'jme-ref-1'", "- id: 'jme-ref-1'\n  version: '5'");
        putRaw(operator, "/api/delete-jobs/" + UUID.randomUUID(), withVersion).then().statusCode(400);
        putRaw(operator, "/api/delete-jobs/" + UUID.randomUUID(), deleteJob("indexMajorVersion: 1\nallowRollback: true"))
                .then().statusCode(400);
    }

    @Test
    void createsTheTasksOfASubmittedJobInTheBackground() {
        String operator = clientCredentialsToken("job-operator-sys");
        String path = "/api/upsert-jobs/" + UUID.randomUUID();

        Map<String, Object> submitted = putYaml(operator, path, upsertJob("indexMajorVersion: 1", "1"), 200);
        assertThat(summary(submitted)).containsEntry("total-references", 2);

        Map<String, Object> prepared = awaitAllTasksCreated(operator, path, 2);
        assertThat(summary(prepared)).containsEntry("open", 2).containsEntry("succeeded", 0).containsEntry("failed", 0);
        assertThat(references(prepared)).extracting(r -> r.get("state")).containsOnly("open");
        assertThat(references(prepared)).allSatisfy(r -> assertThat(r.get("task-id")).isNotNull());
    }

    // The outcomes of the maintenance target policy check itself (rollover/no-rollover/unmanaged/undetermined
    // policies, invalid/missing/non-concrete targets) are already exhaustively covered in
    // jeap-opensearch-index-writer-service's own IsmAppliedPolicyInspectorIT; this end-to-end test only needs to
    // prove that the real deployed job API refuses a target with a rollover policy.
    @Test
    void refusesJobsWhoseTargetHasARolloverPolicy() {
        String target = "jme_transit_decision_v1-000001";
        IsmFixtures ism = new IsmFixtures(openSearchUrl);
        ism.ensureIsmConfigIndexOpen();
        ism.recreatePolicies();
        // Safe on the live index: the rollover action sits in a state reached after ten years only
        ism.attachPolicy(target, IsmFixtures.ROLLOVER_POLICY_ID);
        try {
            ism.awaitPolicyAttached(target, IsmFixtures.ROLLOVER_POLICY_ID);
            String operator = clientCredentialsToken("job-operator-sys");
            int jobsBefore = documentCount(JOB_INDEX);

            Response refused = putRaw(operator, "/api/delete-jobs/" + UUID.randomUUID(), deleteJob("indexMajorVersion: 1"));

            assertThat(refused.statusCode()).as(refused.asString()).isEqualTo(422);
            assertThat(parseYaml(refused)).containsEntry("code", "NOT_ALLOWED");
            assertThat(documentCount(JOB_INDEX)).as("nothing is stored for a refused job").isEqualTo(jobsBefore);
        } finally {
            // Other tests submit jobs for this index, so it must be accepted again before the next test starts
            ism.detachPolicy(target);
            ism.awaitPolicyDetached(target);
            ism.deletePolicies();
        }
    }

    @Test
    void readerRoleRetrievesStoredJobs() {
        String jobId = UUID.randomUUID().toString();
        putYaml(clientCredentialsToken("job-operator-sys"), "/api/delete-jobs/" + jobId, deleteJob("indexMajorVersion: 1"), 200);

        Map<String, Object> report = getYaml(clientCredentialsToken("job-reader-sys"), "/api/delete-jobs/" + jobId, 200);

        assertThat(report).containsEntry("job-id", jobId).containsEntry("job-type", "delete");
        assertThat(references(report)).extracting(r -> r.get("id")).containsExactly("jme-ref-1", "jme-ref-2");
    }

    @Test
    void rejectsInvalidJobRequestsAndProtectsOperationsByRole() {
        String operator = clientCredentialsToken("job-operator-sys");
        String reader = clientCredentialsToken("job-reader-sys");
        String path = "/api/upsert-jobs/" + UUID.randomUUID();

        putRaw(operator, path, upsertJob("indexMajorVersion: 9", "1")).then().statusCode(400);
        putRaw(operator, path, upsertJob("indexMajorVersion: 1", "1").replace("- id: 'jme-ref-2'", "- id: 'jme-ref-1'\n  version: '2'"))
                .then().statusCode(400);
        putRaw(operator, path, "not: [valid").then().statusCode(400);
        given().baseUri(INDEX_WRITER_BASE_URL).auth().oauth2(operator).contentType("application/json").body("{}")
                .put(path).then().statusCode(415);

        // The jEAP security chain checks CSRF before authentication: a PUT without token is refused with 403, a GET with 401
        given().baseUri(INDEX_WRITER_BASE_URL).contentType(YAML).body(upsertJob("", "1").getBytes(UTF_8)).put(path).then().statusCode(403);
        given().baseUri(INDEX_WRITER_BASE_URL).accept(YAML).get(path).then().statusCode(401);
        putRaw(reader, path, upsertJob("", "1")).then().statusCode(403);
        given().baseUri(INDEX_WRITER_BASE_URL).auth().oauth2(accessToken).accept(YAML).get(path).then().statusCode(401);
        given().baseUri(INDEX_WRITER_BASE_URL).auth().oauth2(reader).accept(YAML).get("/api/upsert-jobs/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void publishesJobApiInOpenApiAndSwaggerUi() {
        String docs = given().baseUri(INDEX_WRITER_BASE_URL).auth().oauth2(clientCredentialsToken("job-operator-sys")).get("/api-docs/maintenance-jobs")
                .then().statusCode(200).extract().asString();

        assertThat(docs).contains("/api/upsert-jobs/{jobId}", "/api/delete-jobs/{jobId}", "application/yaml",
                "upsertjob#create", "deletejob#create", "jobreport#read", "NOT_ALLOWED", "originReferences");
        given().baseUri(INDEX_WRITER_BASE_URL).get("/swagger-ui/index.html").then().statusCode(200);
    }

    @Test
    void reusesStoredJobsInAnotherInstanceAndKeepsThemWithMaintenanceDisabled() throws IOException {
        String operator = clientCredentialsToken("job-operator-sys");
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> submitted = putYaml(operator, "/api/upsert-jobs/" + jobId, upsertJob("indexMajorVersion: 1", "17"), 200);
        // Tasks are created in the background after the PUT has answered; count only once they all exist
        awaitAllTasksCreated(operator, "/api/upsert-jobs/" + jobId, 2);
        int jobsBefore = documentCount(JOB_INDEX);
        int tasksBefore = documentCount(TASK_INDEX);
        assertThat(jobsBefore).isPositive();

        List<Integer> ports = reserveFreePorts(2);
        String secondInstanceUrl = "http://localhost:%d/jme-opensearch-index-writer-service".formatted(ports.get(0));
        try {
            startService("jme-opensearch-index-writer-service", secondInstanceUrl, Map.of("server.port", String.valueOf(ports.get(0))));
            Map<String, Object> fromSecondInstance = getYaml(secondInstanceUrl, operator, "/api/upsert-jobs/" + jobId, 200);
            assertThat(fromSecondInstance).containsEntry("started", submitted.get("started"));
            assertThat(target(fromSecondInstance)).isEqualTo(target(submitted));
            assertThat(references(fromSecondInstance)).isEqualTo(references(submitted));
            assertThat(documentCount(JOB_INDEX)).isEqualTo(jobsBefore);
            assertThat(documentCount(TASK_INDEX)).isEqualTo(tasksBefore);

            String disabledUrl = "http://localhost:%d/jme-opensearch-index-writer-service".formatted(ports.get(1));
            startService("jme-opensearch-index-writer-service", disabledUrl, Map.of(
                    "server.port", String.valueOf(ports.get(1)),
                    "jeap.opensearch.indexwriter.maintenance.enabled", "false"));
            given().baseUri(disabledUrl).auth().oauth2(operator).accept(YAML).get("/api/upsert-jobs/" + jobId).then().statusCode(404);
            given().baseUri(disabledUrl).auth().oauth2(operator).contentType(YAML).body(upsertJob("", "1").getBytes(UTF_8))
                    .put("/api/upsert-jobs/" + UUID.randomUUID()).then().statusCode(404);
            assertThat(given().baseUri(disabledUrl).auth().oauth2(operator).get("/api-docs").then().extract().asString())
                    .doesNotContain("/api/upsert-jobs", "/api/delete-jobs");
            assertThat(documentCount(JOB_INDEX)).isEqualTo(jobsBefore);
            assertThat(documentCount(TASK_INDEX)).isEqualTo(tasksBefore);
        } finally {
            // The extra instances join the consumer group of the main index writer; stopping them lets it take over all
            // partitions again before the next test publishes messages
            ports.forEach(OpenSearchExampleIT::stopServiceOnPort);
            KafkaConsumerGroupAwaiter.waitForAssignment(
                    "jme-opensearch-index-writer-service",
                    "jme-create-transit-document",
                    "jme-transit-decision-created");
        }
    }

    // The test base stops started services only after all tests; a graceful stop lets the Kafka consumer leave the group
    private static void stopServiceOnPort(int port) {
        String portArgument = "-Dserver.port=" + port;
        List<ProcessHandle> processes = ProcessHandle.allProcesses()
                .filter(p -> p.info().commandLine().map(c -> c.contains(portArgument)).orElse(false))
                .toList();
        processes.forEach(ProcessHandle::destroy);
        processes.forEach(p -> {
            try {
                p.onExit().get(60, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception _) {
                p.destroyForcibly();
            }
        });
    }

    private static String upsertJob(String majorLine, String versionOfFirst) {
        return """
                message:
                  messageName: JmeTransitDecisionCreatedEvent
                  topicName: jme-transit-decision-created
                operation:
                  indexType: JmeTransitDecision
                  indexOperation: UPSERT
                  %s
                originReferences:
                - id: 'jme-ref-1'
                  version: '%s'
                - id: 'jme-ref-2'
                """.formatted(majorLine, versionOfFirst);
    }

    private static String deleteJob(String majorLine) {
        return """
                indexType: JmeTransitDecision
                %s
                originReferences:
                - id: 'jme-ref-1'
                - id: 'jme-ref-2'
                """.formatted(majorLine);
    }

    private static Response putRaw(String token, String path, String yaml) {
        return given().baseUri(INDEX_WRITER_BASE_URL).auth().oauth2(token).contentType(YAML).body(yaml.getBytes(UTF_8)).put(path);
    }

    private static Map<String, Object> putYaml(String token, String path, String yaml, int expectedStatus) {
        Response response = putRaw(token, path, yaml);
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(expectedStatus);
        return parseYaml(response);
    }

    private static Map<String, Object> getYaml(String token, String path, int expectedStatus) {
        return getYaml(INDEX_WRITER_BASE_URL, token, path, expectedStatus);
    }

    private static Map<String, Object> getYaml(String baseUrl, String token, String path, int expectedStatus) {
        Response response = given().baseUri(baseUrl).auth().oauth2(token).accept(YAML).get(path);
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(expectedStatus);
        return parseYaml(response);
    }

    private static Map<String, Object> parseYaml(Response response) {
        assertThat(response.contentType()).startsWith(YAML);
        return new Yaml().load(response.asString());
    }

    private static Map<String, Object> awaitAllTasksCreated(String token, String path, int expectedTasks) {
        return await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(500)).until(() -> getYaml(token, path, 200),
                report -> Integer.valueOf(expectedTasks).equals(summary(report).get("materialized-tasks")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> summary(Map<String, Object> report) {
        return (Map<String, Object>) report.get("summary");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> target(Map<String, Object> report) {
        return (Map<String, Object>) report.get("target");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> references(Map<String, Object> report) {
        return (List<Map<String, Object>>) report.get("originReferences");
    }

    private int documentCount(String index) {
        given().baseUri(openSearchUrl).post("/" + index + "/_refresh");
        return given().baseUri(openSearchUrl).get("/" + index + "/_count").then().statusCode(200).extract().path("count");
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

    // Guards the documented default: business indices carry no ISM policy unless one is deliberately attached
    private void assertNoIsmPolicyApplied(String physicalIndex) {
        Response explain = given().baseUri(openSearchUrl).get("/_plugins/_ism/explain/" + physicalIndex);
        assertThat(explain.statusCode()).isEqualTo(200);
        assertThat(explain.jsonPath().getInt("total_managed_indices")).isZero();
    }
}
