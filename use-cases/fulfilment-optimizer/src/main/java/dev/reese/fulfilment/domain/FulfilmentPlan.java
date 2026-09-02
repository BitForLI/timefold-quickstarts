package dev.reese.fulfilment.domain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;

@PlanningSolution
public class FulfilmentPlan {

    private String name;

    @ValueRangeProvider(id = "centreRange")
    @ProblemFactCollectionProperty
    private List<FulfilmentCentre> centres = new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<CustomerOrder> orders = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<OrderLineAllocation> allocations = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score;

    private SolverStatus solverStatus;

    public FulfilmentPlan() {
    }

    public FulfilmentPlan(String name, List<FulfilmentCentre> centres, List<CustomerOrder> orders,
            List<OrderLineAllocation> allocations) {
        this.name = name;
        this.centres = centres;
        this.orders = orders;
        this.allocations = allocations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FulfilmentCentre> getCentres() {
        return centres;
    }

    public void setCentres(List<FulfilmentCentre> centres) {
        this.centres = centres;
    }

    public List<CustomerOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<CustomerOrder> orders) {
        this.orders = orders;
    }

    public List<OrderLineAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<OrderLineAllocation> allocations) {
        this.allocations = allocations;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public SolverStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }
}
