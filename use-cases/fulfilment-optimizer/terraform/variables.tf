variable "aws_region" {
  description = "AWS region for the service data plane."
  type        = string
  default     = "ap-southeast-2"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"
}

variable "database_subnet_ids" {
  description = "Private subnet IDs used by the RDS subnet group."
  type        = list(string)
}

variable "database_security_group_ids" {
  description = "Security groups allowing PostgreSQL traffic from the application."
  type        = list(string)
}
