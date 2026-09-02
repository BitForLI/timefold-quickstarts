package dev.reese.fulfilment.messaging;

import java.time.OffsetDateTime;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.reese.fulfilment.persistence.OutboxEventRepository;

@ApplicationScoped
public class OutboxDispatcher {

    private static final int BATCH_SIZE = 20;

    private final OutboxEventRepository repository;
    private final SqsEventPublisher publisher;

    @Inject
    public OutboxDispatcher(OutboxEventRepository repository, SqsEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * SQS is at-least-once. The event ID is propagated so consumers can deduplicate a delivery retried after a crash.
     */
    @Scheduled(every = "${fulfilment.outbox.interval:1s}", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    void dispatch() {
        repository.findUnpublished(BATCH_SIZE).forEach(event -> {
            try {
                publisher.publish(event);
                event.publishedAt = OffsetDateTime.now();
                event.lastError = null;
            } catch (RuntimeException exception) {
                event.attempts++;
                event.lastError = truncate(exception.getMessage());
            }
        });
    }

    private static String truncate(String message) {
        if (message == null) {
            return "Unknown event publishing failure";
        }
        return message.length() <= 2_000 ? message : message.substring(0, 2_000);
    }
}
