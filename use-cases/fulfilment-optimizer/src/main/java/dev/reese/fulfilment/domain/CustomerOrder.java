package dev.reese.fulfilment.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;

public class CustomerOrder {

    @PlanningId
    private String id;
    private String deliveryRegion;
    private int maxDeliveryDays;
    private int priority;

    public CustomerOrder() {
    }

    public CustomerOrder(String id, String deliveryRegion, int maxDeliveryDays, int priority) {
        this.id = id;
        this.deliveryRegion = deliveryRegion;
        this.maxDeliveryDays = maxDeliveryDays;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeliveryRegion() {
        return deliveryRegion;
    }

    public void setDeliveryRegion(String deliveryRegion) {
        this.deliveryRegion = deliveryRegion;
    }

    public int getMaxDeliveryDays() {
        return maxDeliveryDays;
    }

    public void setMaxDeliveryDays(int maxDeliveryDays) {
        this.maxDeliveryDays = maxDeliveryDays;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return id;
    }
}
