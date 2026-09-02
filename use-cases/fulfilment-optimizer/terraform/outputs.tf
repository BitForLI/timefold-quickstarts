output "database_endpoint" {
  description = "PostgreSQL writer endpoint."
  value       = aws_db_instance.postgres.address
}

output "event_queue_url" {
  description = "SQS URL consumed by downstream services."
  value       = aws_sqs_queue.fulfilment_events.url
}

output "event_queue_arn" {
  description = "SQS ARN for IAM policy wiring."
  value       = aws_sqs_queue.fulfilment_events.arn
}
