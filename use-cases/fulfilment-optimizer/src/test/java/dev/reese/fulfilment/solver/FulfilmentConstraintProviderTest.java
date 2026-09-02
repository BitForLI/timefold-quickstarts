package dev.reese.fulfilment.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import dev.reese.fulfilment.domain.CustomerOrder;
import dev.reese.fulfilment.domain.FulfilmentCentre;
import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.domain.OrderLineAllocation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FulfilmentConstraintProviderTest {

    @Inject
    ConstraintVerifier<FulfilmentConstraintProvider, FulfilmentPlan> constraintVerifier;

    @Test
    void capacityPenalizesOnlyTheOverage() {
        FulfilmentCentre centre = centre("SYD", 10, Map.of("SKU-1", 20), 1, 500);
        CustomerOrder order = new CustomerOrder("order-1", "NSW", 2, 1);
        OrderLineAllocation first = allocation("line-1", order, "SKU-1", 7, centre);
        OrderLineAllocation second = allocation("line-2", order, "SKU-1", 6, centre);

        constraintVerifier.verifyThat(FulfilmentConstraintProvider::centreCapacity)
                .given(first, second)
                .penalizesBy(3);
    }

    @Test
    void inventoryAggregatesDemandByCentreAndSku() {
        FulfilmentCentre centre = centre("SYD", 100, Map.of("SKU-1", 8), 1, 500);
        CustomerOrder firstOrder = new CustomerOrder("order-1", "NSW", 2, 1);
        CustomerOrder secondOrder = new CustomerOrder("order-2", "NSW", 2, 1);

        constraintVerifier.verifyThat(FulfilmentConstraintProvider::skuInventory)
                .given(allocation("line-1", firstOrder, "SKU-1", 5, centre),
                        allocation("line-2", secondOrder, "SKU-1", 7, centre))
                .penalizesBy(4);
    }

    @Test
    void slaPenaltyReflectsLatenessQuantityAndPriority() {
        FulfilmentCentre centre = centre("PER", 100, Map.of("SKU-1", 10), 5, 500);
        CustomerOrder order = new CustomerOrder("order-1", "NSW", 2, 3);

        constraintVerifier.verifyThat(FulfilmentConstraintProvider::deliverySla)
                .given(allocation("line-1", order, "SKU-1", 4, centre))
                .penalizesBy(36);
    }

    @Test
    void splitPenaltyCountsOnlyAdditionalCentres() {
        FulfilmentCentre sydney = centre("SYD", 100, Map.of("A", 10, "B", 10), 1, 500);
        FulfilmentCentre melbourne = centre("MEL", 100, Map.of("A", 10, "B", 10), 1, 450);
        CustomerOrder order = new CustomerOrder("order-1", "NSW", 2, 1);

        constraintVerifier.verifyThat(FulfilmentConstraintProvider::orderSplit)
                .given(allocation("line-1", order, "A", 1, sydney),
                        allocation("line-2", order, "B", 1, melbourne))
                .penalizesBy(FulfilmentConstraintProvider.ORDER_SPLIT_PENALTY);
    }

    @Test
    void deliveryCostUsesUnitsAndLaneCost() {
        FulfilmentCentre centre = centre("SYD", 100, Map.of("SKU-1", 10), 1, 275);
        CustomerOrder order = new CustomerOrder("order-1", "NSW", 2, 1);

        constraintVerifier.verifyThat(FulfilmentConstraintProvider::deliveryCost)
                .given(allocation("line-1", order, "SKU-1", 4, centre))
                .penalizesBy(1_100);
    }

    private static FulfilmentCentre centre(String id, int capacity, Map<String, Integer> inventory,
            int deliveryDays, int costCents) {
        FulfilmentCentre centre = new FulfilmentCentre(id, capacity, inventory,
                Map.of("NSW", deliveryDays), Map.of("NSW", costCents));
        assertThat(centre.getId()).isEqualTo(id);
        return centre;
    }

    private static OrderLineAllocation allocation(String id, CustomerOrder order, String sku, int quantity,
            FulfilmentCentre centre) {
        OrderLineAllocation allocation = new OrderLineAllocation(id, order, sku, quantity);
        allocation.setFulfilmentCentre(centre);
        return allocation;
    }
}
