package dev.reese.fulfilment.messaging;

import java.net.URI;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import dev.reese.fulfilment.persistence.OutboxEventEntity;

@ApplicationScoped
public class SqsEventPublisher {

    private static final Logger LOGGER = Logger.getLogger(SqsEventPublisher.class);

    @ConfigProperty(name = "fulfilment.events.sqs.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "fulfilment.events.sqs.queue-url", defaultValue = "")
    Optional<String> queueUrl;

    @ConfigProperty(name = "fulfilment.events.sqs.region", defaultValue = "ap-southeast-2")
    String region;

    @ConfigProperty(name = "fulfilment.events.sqs.endpoint-override", defaultValue = "")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "fulfilment.events.sqs.access-key", defaultValue = "test")
    String accessKey;

    @ConfigProperty(name = "fulfilment.events.sqs.secret-key", defaultValue = "test")
    String secretKey;

    private volatile SqsClient client;

    public void publish(OutboxEventEntity event) {
        if (!enabled) {
            LOGGER.infof("Outbox event %s (%s) ready for publication.", event.id, event.eventType);
            return;
        }
        if (queueUrl.isEmpty() || queueUrl.get().isBlank()) {
            throw new IllegalStateException("SQS publishing is enabled but the queue URL is empty");
        }
        sqsClient().sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl.get())
                .messageBody(event.payloadJson)
                .messageAttributes(
                        java.util.Map.of(
                                "eventId", stringAttribute(event.id),
                                "eventType", stringAttribute(event.eventType),
                                "aggregateId", stringAttribute(event.aggregateId)))
                .build());
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }

    private SqsClient sqsClient() {
        SqsClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                var builder = SqsClient.builder().region(Region.of(region));
                if (endpointOverride.isPresent() && !endpointOverride.get().isBlank()) {
                    builder.endpointOverride(URI.create(endpointOverride.get()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKey, secretKey)));
                }
                client = builder.build();
            }
            return client;
        }
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}
