package dev.reese.fulfilment.persistence;

import java.time.OffsetDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity extends PanacheEntityBase {

    @Id
    @Column(length = 36)
    public String id;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    public String aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    public String eventType;

    @Lob
    @Column(name = "payload_json", nullable = false)
    public String payloadJson;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "published_at")
    public OffsetDateTime publishedAt;

    @Column(nullable = false)
    public int attempts;

    @Column(name = "last_error", length = 2_000)
    public String lastError;

    @Version
    public long version;
}
