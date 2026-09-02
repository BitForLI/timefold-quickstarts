package dev.reese.fulfilment.persistence;

import java.time.OffsetDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "plan_job", uniqueConstraints = @UniqueConstraint(
        name = "uk_plan_job_idempotency_key", columnNames = "idempotency_key"))
public class PlanJobEntity extends PanacheEntityBase {

    @Id
    @Column(length = 36)
    public String id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    public String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    public String requestHash;

    @Lob
    @Column(name = "request_json", nullable = false)
    public String requestJson;

    @Lob
    @Column(name = "solution_json")
    public String solutionJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public PlanJobStatus status;

    @Column(name = "score", length = 96)
    public String score;

    @Column(name = "error_message", length = 2_000)
    public String errorMessage;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @Version
    public long version;
}
