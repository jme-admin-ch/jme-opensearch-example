package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
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

    private static String accessToken;

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
        assertThat(originId).isNotBlank();
        assertThat(goodsDescription).isNotBlank();

        String firstToken = goodsDescription.split(" ")[0];
        await().atMost(TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Response inspection = given()
                    .baseUri(INSPECTION_BASE_URL)
                    .auth().oauth2(accessToken)
                    .queryParam("goodsDescription", firstToken)
                    .when()
                    .get("/api/transitdocuments");

            assertThat(inspection.statusCode()).isEqualTo(200);
            assertThat(inspection.jsonPath().getList("origin.id", String.class)).contains(originId);
        });
    }

    @Test
    void createsAndIndexesTransitDecision() {
        Response created = given()
                .baseUri(RESOURCE_BASE_URL)
                .when()
                .post("/api/transitdescisions")
                .then()
                .statusCode(201)
                .extract().response();

        String originId = created.path("searchItem.origin.id");
        String decidedBy = created.path("searchItem.data.decided_by");
        assertThat(originId).isNotBlank();
        assertThat(decidedBy).isNotBlank();

        await().atMost(TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Response inspection = given()
                    .baseUri(INSPECTION_BASE_URL)
                    .auth().oauth2(accessToken)
                    .queryParam("decidedBy", decidedBy)
                    .when()
                    .get("/api/transitdecisions");

            assertThat(inspection.statusCode()).isEqualTo(200);
            assertThat(inspection.jsonPath().getList("origin.id", String.class)).contains(originId);
        });
    }
}
