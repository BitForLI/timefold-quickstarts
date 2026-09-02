package dev.reese.fulfilment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reese.fulfilment.domain.FulfilmentCentre;
import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.domain.OrderLineAllocation;
import dev.reese.fulfilment.persistence.PlanJobEntity;
import dev.reese.fulfilment.persistence.PlanJobRepository;
import dev.reese.fulfilment.persistence.PlanJobStatus;
import dev.reese.fulfilment.persistence.PlanJobStore;
import dev.reese.fulfilment.rest.JobSubmissionResponse;
import dev.reese.fulfilment.rest.PlanJobResponse;
import dev.reese.fulfilment.solver.GreedyFulfilmentAllocator;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FulfilmentPlanningService {

    private final SolverManager<FulfilmentPlan> solverManager;
    private final SolutionManager<FulfilmentPlan, HardSoftScore> solutionManager;
    private final GreedyFulfilmentAllocator baselineAllocator;
    private final PlanJobStore jobStore;
    private final PlanJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public FulfilmentPlanningService(SolverManager<FulfilmentPlan> solverManager,
            SolutionManager<FulfilmentPlan, HardSoftScore> solutionManager,
            GreedyFulfilmentAllocator baselineAllocator, PlanJobStore jobStore,
            PlanJobRepository jobRepository, ObjectMapper objectMapper) {
        this.solverManager = solverManager;
        this.solutionManager = solutionManager;
        this.baselineAllocator = baselineAllocator;
        this.jobStore = jobStore;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    void resumeInterruptedJobs(@Observes StartupEvent event) {
        jobRepository.findActive().forEach(this::startSolver);
    }

    public JobSubmissionResponse submit(String idempotencyKey, FulfilmentPlan problem) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        }
        validate(problem);
        String requestJson = write(problem);
        PlanJobStore.CreateResult result = jobStore.createOrFind(idempotencyKey, sha256(requestJson), requestJson);
        if (!result.replayed()) {
            startSolver(result.job());
        }
        return new JobSubmissionResponse(result.job().id, result.job().status, result.replayed());
    }

    public PlanJobResponse get(String jobId) {
        PlanJobEntity job = jobStore.find(jobId);
        SolverStatus solverStatus = solverManager.getSolverStatus(jobId);
        if ((job.status == PlanJobStatus.QUEUED || job.status == PlanJobStatus.SOLVING)
                && solverStatus == SolverStatus.NOT_SOLVING && job.solutionJson != null) {
            jobStore.complete(jobId);
            job = jobStore.find(jobId);
        }
        FulfilmentPlan solution = job.solutionJson == null ? null : read(job.solutionJson);
        if (solution != null) {
            solution.setSolverStatus(solverStatus);
        }
        return new PlanJobResponse(job.id, job.status, solverStatus.name(), job.score,
                job.createdAt, job.updatedAt, solution, job.errorMessage);
    }

    public PlanJobResponse cancel(String jobId) {
        jobStore.find(jobId);
        solverManager.terminateEarly(jobId);
        jobStore.cancel(jobId);
        return get(jobId);
    }

    public FulfilmentPlan baseline(FulfilmentPlan problem) {
        validate(problem);
        baselineAllocator.allocate(problem);
        solutionManager.update(problem);
        problem.setSolverStatus(SolverStatus.NOT_SOLVING);
        return problem;
    }

    private void startSolver(PlanJobEntity job) {
        solverManager.solveBuilder()
                .withProblemId(job.id)
                .withProblemFinder(ignored -> initialSolution(job.requestJson))
                .withBestSolutionEventConsumer(event -> {
                    FulfilmentPlan solution = event.solution();
                    jobStore.recordBestSolution(job.id, write(solution), String.valueOf(solution.getScore()));
                })
                .withExceptionHandler((ignored, exception) -> jobStore.fail(job.id, exception))
                .run();
    }

    private FulfilmentPlan initialSolution(String requestJson) {
        FulfilmentPlan plan = read(requestJson);
        plan.getAllocations().forEach(allocation -> allocation.setFulfilmentCentre(null));
        try {
            baselineAllocator.allocate(plan);
        } catch (IllegalStateException infeasibleForGreedyBaseline) {
            plan.getAllocations().forEach(allocation -> allocation.setFulfilmentCentre(null));
        }
        return plan;
    }

    private void validate(FulfilmentPlan plan) {
        if (plan == null || plan.getCentres() == null || plan.getCentres().isEmpty()) {
            throw new IllegalArgumentException("At least one fulfilment centre is required");
        }
        if (plan.getOrders() == null || plan.getOrders().isEmpty()) {
            throw new IllegalArgumentException("At least one customer order is required");
        }
        if (plan.getAllocations() == null || plan.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("At least one order line is required");
        }
        Set<String> centreIds = uniqueIds(plan.getCentres().stream().map(FulfilmentCentre::getId).toList(), "centre");
        Set<String> orderIds = uniqueIds(plan.getOrders().stream().map(order -> order.getId()).toList(), "order");
        uniqueIds(plan.getAllocations().stream().map(OrderLineAllocation::getId).toList(), "order line");
        for (FulfilmentCentre centre : plan.getCentres()) {
            if (centre.getDailyCapacityUnits() <= 0 || centre.getInventoryBySku() == null
                    || centre.getDeliveryDaysByRegion() == null || centre.getShippingCostCentsByRegion() == null) {
                throw new IllegalArgumentException("Centre " + centre.getId() + " has invalid capacity or lane data");
            }
            if (!centreIds.contains(centre.getId()) || centre.getInventoryBySku().values().stream().anyMatch(value -> value < 0)) {
                throw new IllegalArgumentException("Centre " + centre.getId() + " has invalid inventory");
            }
        }
        for (OrderLineAllocation allocation : plan.getAllocations()) {
            if (allocation.getOrder() == null || !orderIds.contains(allocation.getOrder().getId())
                    || allocation.getSku() == null || allocation.getSku().isBlank() || allocation.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order line " + allocation.getId() + " is invalid");
            }
        }
    }

    private Set<String> uniqueIds(java.util.List<String> ids, String type) {
        if (ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("Every " + type + " must have an ID");
        }
        Set<String> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new IllegalArgumentException("Duplicate " + type + " ID");
        }
        return unique;
    }

    private String write(FulfilmentPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize fulfilment plan", exception);
        }
    }

    private FulfilmentPlan read(String json) {
        try {
            return objectMapper.readValue(json, FulfilmentPlan.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize persisted fulfilment plan", exception);
        }
    }

    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
