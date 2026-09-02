package dev.reese.fulfilment.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.common.PlanningId;

public class FulfilmentCentre {

    @PlanningId
    private String id;
    private int dailyCapacityUnits;
    private Map<String, Integer> inventoryBySku = new LinkedHashMap<>();
    private Map<String, Integer> deliveryDaysByRegion = new LinkedHashMap<>();
    private Map<String, Integer> shippingCostCentsByRegion = new LinkedHashMap<>();

    public FulfilmentCentre() {
    }

    public FulfilmentCentre(String id, int dailyCapacityUnits, Map<String, Integer> inventoryBySku,
            Map<String, Integer> deliveryDaysByRegion, Map<String, Integer> shippingCostCentsByRegion) {
        this.id = id;
        this.dailyCapacityUnits = dailyCapacityUnits;
        this.inventoryBySku = new LinkedHashMap<>(inventoryBySku);
        this.deliveryDaysByRegion = new LinkedHashMap<>(deliveryDaysByRegion);
        this.shippingCostCentsByRegion = new LinkedHashMap<>(shippingCostCentsByRegion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDailyCapacityUnits() {
        return dailyCapacityUnits;
    }

    public void setDailyCapacityUnits(int dailyCapacityUnits) {
        this.dailyCapacityUnits = dailyCapacityUnits;
    }

    public Map<String, Integer> getInventoryBySku() {
        return inventoryBySku;
    }

    public void setInventoryBySku(Map<String, Integer> inventoryBySku) {
        this.inventoryBySku = inventoryBySku;
    }

    public Map<String, Integer> getDeliveryDaysByRegion() {
        return deliveryDaysByRegion;
    }

    public void setDeliveryDaysByRegion(Map<String, Integer> deliveryDaysByRegion) {
        this.deliveryDaysByRegion = deliveryDaysByRegion;
    }

    public Map<String, Integer> getShippingCostCentsByRegion() {
        return shippingCostCentsByRegion;
    }

    public void setShippingCostCentsByRegion(Map<String, Integer> shippingCostCentsByRegion) {
        this.shippingCostCentsByRegion = shippingCostCentsByRegion;
    }

    public int inventoryFor(String sku) {
        return inventoryBySku.getOrDefault(sku, 0);
    }

    public int deliveryDaysFor(String region) {
        return deliveryDaysByRegion.getOrDefault(region, Integer.MAX_VALUE / 4);
    }

    public int shippingCostCentsFor(String region) {
        return shippingCostCentsByRegion.getOrDefault(region, 1_000_000);
    }

    @Override
    public String toString() {
        return id;
    }
}
