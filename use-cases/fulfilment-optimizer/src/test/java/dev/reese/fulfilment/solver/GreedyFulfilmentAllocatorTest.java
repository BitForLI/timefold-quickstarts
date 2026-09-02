package dev.reese.fulfilment.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import dev.reese.fulfilment.domain.CustomerOrder;
import dev.reese.fulfilment.domain.FulfilmentCentre;
import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.domain.OrderLineAllocation;
import org.junit.jupiter.api.Test;

class GreedyFulfilmentAllocatorTest {

    private final GreedyFulfilmentAllocator allocator = new GreedyFulfilmentAllocator();

    @Test
    void allocationIsDeterministicAndPrefersAvoidingAnOrderSplit() {
        FulfilmentCentre sydney = new FulfilmentCentre("SYD", 20, Map.of("A", 10, "B", 10),
                Map.of("NSW", 1), Map.of("NSW", 400));
        FulfilmentCentre melbourne = new FulfilmentCentre("MEL", 20, Map.of("A", 10, "B", 10),
                Map.of("NSW", 2), Map.of("NSW", 250));
        CustomerOrder order = new CustomerOrder("order-1", "NSW", 2, 5);
        OrderLineAllocation first = new OrderLineAllocation("line-1", order, "A", 2);
        OrderLineAllocation second = new OrderLineAllocation("line-2", order, "B", 2);
        FulfilmentPlan plan = new FulfilmentPlan("baseline", List.of(sydney, melbourne), List.of(order),
                List.of(first, second));

        allocator.allocate(plan);

        assertThat(first.getFulfilmentCentre()).isEqualTo(melbourne);
        assertThat(second.getFulfilmentCentre()).isEqualTo(melbourne);
    }
}
