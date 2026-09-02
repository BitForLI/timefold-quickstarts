package dev.reese.fulfilment.solver;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countDistinct;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.sum;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import dev.reese.fulfilment.domain.OrderLineAllocation;

public class FulfilmentConstraintProvider implements ConstraintProvider {

    static final int ORDER_SPLIT_PENALTY = 10_000;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                centreCapacity(constraintFactory),
                skuInventory(constraintFactory),
                deliverySla(constraintFactory),
                deliveryCost(constraintFactory),
                orderSplit(constraintFactory),
                balanceCentreLoad(constraintFactory)
        };
    }

    Constraint centreCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .groupBy(OrderLineAllocation::getFulfilmentCentre, sum(OrderLineAllocation::getQuantity))
                .filter((centre, allocatedUnits) -> allocatedUnits > centre.getDailyCapacityUnits())
                .penalize(HardSoftScore.ONE_HARD,
                        (centre, allocatedUnits) -> allocatedUnits - centre.getDailyCapacityUnits())
                .asConstraint("Centre capacity");
    }

    Constraint skuInventory(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .groupBy(OrderLineAllocation::getFulfilmentCentre,
                        OrderLineAllocation::getSku,
                        sum(OrderLineAllocation::getQuantity))
                .filter((centre, sku, allocatedUnits) -> allocatedUnits > centre.inventoryFor(sku))
                .penalize(HardSoftScore.ONE_HARD,
                        (centre, sku, allocatedUnits) -> allocatedUnits - centre.inventoryFor(sku))
                .asConstraint("SKU inventory");
    }

    Constraint deliverySla(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .filter(allocation -> allocation.getFulfilmentCentre()
                        .deliveryDaysFor(allocation.getOrder().getDeliveryRegion()) > allocation.getOrder().getMaxDeliveryDays())
                .penalize(HardSoftScore.ONE_HARD, allocation -> {
                    int actualDays = allocation.getFulfilmentCentre()
                            .deliveryDaysFor(allocation.getOrder().getDeliveryRegion());
                    int daysLate = Math.min(365, actualDays - allocation.getOrder().getMaxDeliveryDays());
                    return daysLate * allocation.getQuantity() * Math.max(1, allocation.getOrder().getPriority());
                })
                .asConstraint("Delivery SLA");
    }

    Constraint deliveryCost(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .penalize(HardSoftScore.ONE_SOFT,
                        allocation -> allocation.getQuantity() * allocation.getFulfilmentCentre()
                                .shippingCostCentsFor(allocation.getOrder().getDeliveryRegion()))
                .asConstraint("Delivery cost");
    }

    Constraint orderSplit(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .groupBy(allocation -> allocation.getOrder().getId(),
                        countDistinct(OrderLineAllocation::getFulfilmentCentre))
                .filter((orderId, centreCount) -> centreCount > 1)
                .penalize(HardSoftScore.ONE_SOFT,
                        (orderId, centreCount) -> (centreCount - 1) * ORDER_SPLIT_PENALTY)
                .asConstraint("Order split");
    }

    Constraint balanceCentreLoad(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(OrderLineAllocation.class)
                .groupBy(OrderLineAllocation::getFulfilmentCentre, sum(OrderLineAllocation::getQuantity))
                .penalize(HardSoftScore.ONE_SOFT, (centre, allocatedUnits) -> {
                    long scaledLoad = 100L * allocatedUnits / Math.max(1, centre.getDailyCapacityUnits());
                    return (int) Math.min(Integer.MAX_VALUE, scaledLoad * scaledLoad);
                })
                .asConstraint("Balance centre load");
    }
}
