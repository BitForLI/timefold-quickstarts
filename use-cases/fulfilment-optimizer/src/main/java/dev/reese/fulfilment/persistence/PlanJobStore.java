package dev.reese.fulfilment.persistence;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PlanJobStore {

    private final PlanJobRepository jobRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public PlanJobStore(PlanJobRepository jobRepository, OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateResult createOrFind(String idempotencyKey, String requestHash, String requestJson) {
        Optional<PlanJobEntity> existing = jobRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PlanJobEntity job = existing.get();
            if (!job.requestHash.equals(requestHash)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return new CreateResult(job, true);
        }

        OffsetDateTime now = OffsetDateTime.now();
        PlanJobEntity job = new PlanJobEntity();
        job.id = UUID.randomUUID().toString();
        job.idempotencyKey = idempotencyKey;
        job.requestHash = requestHash;
        job.requestJson = requestJson;
        job.status = PlanJobStatus.QUEUED;
        job.createdAt = now;
        job.updatedAt = now;
        jobRepository.persist(job);
        appendEvent(job.id, "FULFILMENT_PLAN_SUBMITTED", Map.of("jobId", job.id, "status", job.status));
        return new CreateResult(job, false);
    }

    @Transactional
    public void recordBestSolution(String jobId, String solutionJson, String score) {
        PlanJobEntity job = require(jobId);
        if (job.status == PlanJobStatus.CANCELLED || job.status == PlanJobStatus.FAILED) {
            return;
        }
        job.solutionJson = solutionJson;
        job.score = score;
        job.status = PlanJobStatus.SOLVING;
        job.updatedAt = OffsetDateTime.now();
    }

    @Transactional
    public void complete(String jobId) {
        PlanJobEntity job = require(jobId);
        if (job.status == PlanJobStatus.COMPLETED || job.status == PlanJobStatus.FAILED
                || job.status == PlanJobStatus.CANCELLED || job.solutionJson == null) {
            return;
        }
        job.status = PlanJobStatus.COMPLETED;
        job.updatedAt = OffsetDateTime.now();
        appendEvent(job.id, "FULFILMENT_PLAN_COMPLETED",
                Map.of("jobId", job.id, "status", job.status, "score", String.valueOf(job.score)));
    }

    @Transactional
    public void fail(String jobId, Throwable failure) {
        PlanJobEntity job = require(jobId);
        job.status = PlanJobStatus.FAILED;
        job.errorMessage = truncate(failure.getMessage(), 2_000);
        job.updatedAt = OffsetDateTime.now();
        appendEvent(job.id, "FULFILMENT_PLAN_FAILED",
                Map.of("jobId", job.id, "status", job.status, "error", String.valueOf(job.errorMessage)));
    }

    @Transactional
    public void cancel(String jobId) {
        PlanJobEntity job = require(jobId);
        if (job.status == PlanJobStatus.COMPLETED || job.status == PlanJobStatus.FAILED) {
            return;
        }
        job.status = PlanJobStatus.CANCELLED;
        job.updatedAt = OffsetDateTime.now();
        appendEvent(job.id, "FULFILMENT_PLAN_CANCELLED", Map.of("jobId", job.id, "status", job.status));
    }

    @Transactional
    public PlanJobEntity find(String jobId) {
        return require(jobId);
    }

    private PlanJobEntity require(String jobId) {
        return jobRepository.findByIdOptional(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    private void appendEvent(String jobId, String eventType, Map<String, ?> payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.id = UUID.randomUUID().toString();
        event.aggregateId = jobId;
        event.eventType = eventType;
        try {
            event.payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
        event.createdAt = OffsetDateTime.now();
        event.attempts = 0;
        outboxRepository.persist(event);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "Unknown solver failure";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record CreateResult(PlanJobEntity job, boolean replayed) {
    }
}
