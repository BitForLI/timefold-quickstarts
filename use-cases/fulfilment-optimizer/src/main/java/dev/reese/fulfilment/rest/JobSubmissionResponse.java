package dev.reese.fulfilment.rest;

import dev.reese.fulfilment.persistence.PlanJobStatus;

public record JobSubmissionResponse(String jobId, PlanJobStatus status, boolean replayed) {
}
