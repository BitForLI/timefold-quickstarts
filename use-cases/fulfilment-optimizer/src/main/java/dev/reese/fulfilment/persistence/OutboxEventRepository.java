package dev.reese.fulfilment.persistence;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepositoryBase<OutboxEventEntity, String> {

    public List<OutboxEventEntity> findUnpublished(int limit) {
        return find("publishedAt is null order by createdAt").page(0, limit).list();
    }
}
