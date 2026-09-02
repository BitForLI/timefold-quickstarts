package dev.reese.fulfilment.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class OrderLineAllocation {

    @PlanningId
    private String id;
    private CustomerOrder order;
    private String sku;
    private int quantity;

    @PlanningVariable(valueRangeProviderRefs = "centreRange")
    private FulfilmentCentre fulfilmentCentre;

    public OrderLineAllocation() {
    }

    public OrderLineAllocation(String id, CustomerOrder order, String sku, int quantity) {
        this.id = id;
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public FulfilmentCentre getFulfilmentCentre() {
        return fulfilmentCentre;
    }

    public void setFulfilmentCentre(FulfilmentCentre fulfilmentCentre) {
        this.fulfilmentCentre = fulfilmentCentre;
    }
}
