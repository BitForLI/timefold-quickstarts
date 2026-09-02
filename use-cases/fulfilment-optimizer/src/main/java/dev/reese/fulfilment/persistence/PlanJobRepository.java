package dev.reese.fulfilment.persistence;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlanJobRepository implements PanacheRepositoryBase<PlanJobEntity, String> {

    public Optional<PlanJobEntity> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    public List<PlanJobEntity> findActive() {
        return list("status in ?1", List.of(PlanJobStatus.QUEUED, PlanJobStatus.SOLVING));
    }
}
