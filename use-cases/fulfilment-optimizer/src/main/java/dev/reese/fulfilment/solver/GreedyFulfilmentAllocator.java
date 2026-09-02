package dev.reese.fulfilment.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import dev.reese.fulfilment.domain.FulfilmentCentre;
import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.domain.OrderLineAllocation;

/**
 * Deterministic, capacity-aware baseline used to compare solver quality and provide a predictable fallback.
 */
@ApplicationScoped
public class GreedyFulfilmentAllocator {

    public FulfilmentPlan allocate(FulfilmentPlan plan) {
        Map<String, Integer> remainingCapacity = new HashMap<>();
        Map<String, Map<String, Integer>> remainingInventory = new HashMap<>();
        for (FulfilmentCentre centre : plan.getCentres()) {
            remainingCapacity.put(centre.getId(), centre.getDailyCapacityUnits());
            remainingInventory.put(centre.getId(), new HashMap<>(centre.getInventoryBySku()));
        }

        Map<String, Set<String>> centresUsedByOrder = new HashMap<>();
        List<OrderLineAllocation> orderedAllocations = new ArrayList<>(plan.getAllocations());
        orderedAllocations.sort(Comparator
                .comparingInt((OrderLineAllocation allocation) -> allocation.getOrder().getPriority()).reversed()
                .thenComparing(Comparator.comparingInt(OrderLineAllocation::getQuantity).reversed())
                .thenComparing(OrderLineAllocation::getId));

        for (OrderLineAllocation allocation : orderedAllocations) {
            FulfilmentCentre chosenCentre = plan.getCentres().stream()
                    .filter(centre -> isFeasible(allocation, centre, remainingCapacity, remainingInventory))
                    .min(Comparator
                            .comparingLong((FulfilmentCentre centre) -> candidateCost(
                                    allocation, centre, centresUsedByOrder))
                            .thenComparing(FulfilmentCentre::getId))
                    .orElseThrow(() -> new IllegalStateException(
                            "No feasible centre for order line " + allocation.getId()));

            allocation.setFulfilmentCentre(chosenCentre);
            remainingCapacity.compute(chosenCentre.getId(), (id, value) -> value - allocation.getQuantity());
            remainingInventory.get(chosenCentre.getId()).compute(allocation.getSku(),
                    (sku, value) -> value - allocation.getQuantity());
            centresUsedByOrder.computeIfAbsent(allocation.getOrder().getId(), ignored -> new HashSet<>())
                    .add(chosenCentre.getId());
        }
        return plan;
    }

    private boolean isFeasible(OrderLineAllocation allocation, FulfilmentCentre centre,
            Map<String, Integer> remainingCapacity, Map<String, Map<String, Integer>> remainingInventory) {
        return centre.deliveryDaysFor(allocation.getOrder().getDeliveryRegion()) <= allocation.getOrder().getMaxDeliveryDays()
                && remainingCapacity.get(centre.getId()) >= allocation.getQuantity()
                && remainingInventory.get(centre.getId()).getOrDefault(allocation.getSku(), 0) >= allocation.getQuantity();
    }

    private long candidateCost(OrderLineAllocation allocation, FulfilmentCentre centre,
            Map<String, Set<String>> centresUsedByOrder) {
        Set<String> usedCentres = centresUsedByOrder.getOrDefault(allocation.getOrder().getId(), Set.of());
        long splitCost = !usedCentres.isEmpty() && !usedCentres.contains(centre.getId())
                ? FulfilmentConstraintProvider.ORDER_SPLIT_PENALTY
                : 0;
        return splitCost + (long) allocation.getQuantity()
                * centre.shippingCostCentsFor(allocation.getOrder().getDeliveryRegion());
    }
}
