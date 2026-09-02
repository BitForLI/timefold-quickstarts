locals {
  name = "fulfilment-optimizer-${var.environment}"
  tags = {
    Application = "fulfilment-optimizer"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

resource "aws_sqs_queue" "fulfilment_events_dlq" {
  name                      = "${local.name}-dlq"
  message_retention_seconds = 1209600
  tags                      = local.tags
}

resource "aws_sqs_queue" "fulfilment_events" {
  name                       = "${local.name}-events"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 345600
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.fulfilment_events_dlq.arn
    maxReceiveCount     = 5
  })
  tags = local.tags
}

resource "random_password" "database" {
  length  = 32
  special = false
}

resource "aws_db_subnet_group" "this" {
  name       = local.name
  subnet_ids = var.database_subnet_ids
  tags       = local.tags
}

resource "aws_db_instance" "postgres" {
  identifier                   = local.name
  engine                       = "postgres"
  engine_version               = "16"
  instance_class               = "db.t4g.micro"
  allocated_storage            = 20
  max_allocated_storage        = 100
  storage_encrypted            = true
  db_name                      = "fulfilment"
  username                     = "fulfilment_app"
  password                     = random_password.database.result
  db_subnet_group_name         = aws_db_subnet_group.this.name
  vpc_security_group_ids       = var.database_security_group_ids
  backup_retention_period      = 7
  deletion_protection          = true
  performance_insights_enabled = true
  skip_final_snapshot          = false
  final_snapshot_identifier    = "${local.name}-final"
  tags                         = local.tags
}
