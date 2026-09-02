package dev.reese.fulfilment.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FulfilmentPlanResourceTest {

    @Test
    void baselineReturnsAFeasibleDeterministicAssignment() {
        given()
                .contentType("application/json")
                .body(samplePlan())
                .when().post("/api/fulfilment-plans/baseline")
                .then()
                .statusCode(200)
                .body("score", notNullValue())
                .body("allocations[0].fulfilmentCentre.id", equalTo("SYD"))
                .body("allocations[2].fulfilmentCentre.id", equalTo("MEL"));
    }

    @Test
    void submitIsIdempotentForTheSameKeyAndPayload() {
        String idempotencyKey = "api-test-idempotency-key";
        String jobId = given()
                .contentType("application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body(samplePlan())
                .when().post("/api/fulfilment-plans")
                .then()
                .statusCode(202)
                .body("replayed", equalTo(false))
                .extract().path("jobId");

        given()
                .contentType("application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body(samplePlan())
                .when().post("/api/fulfilment-plans")
                .then()
                .statusCode(202)
                .body("jobId", equalTo(jobId))
                .body("replayed", equalTo(true));
    }

    @Test
    void submitRejectsAMissingIdempotencyKey() {
        given()
                .contentType("application/json")
                .body(samplePlan())
                .when().post("/api/fulfilment-plans")
                .then()
                .statusCode(400)
                .body("message", equalTo("Idempotency-Key header is required"));
    }

    private String samplePlan() {
        try (var stream = getClass().getResourceAsStream("/sample-plan.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing sample plan fixture");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read sample plan fixture", exception);
        }
    }
}
