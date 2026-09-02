package dev.reese.fulfilment.rest;

import java.time.OffsetDateTime;

import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.persistence.PlanJobStatus;

public record PlanJobResponse(String jobId, PlanJobStatus status, String solverStatus, String score,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, FulfilmentPlan solution, String error) {
}
