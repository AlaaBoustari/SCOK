resource "aws_ecs_service" "scok" {
  name            = "scok"
  cluster         = aws_ecs_cluster.scok.id
  task_definition = aws_ecs_task_definition.scok.arn
  desired_count   = 1

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  health_check_grace_period_seconds  = 90

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.ec2.name
    weight            = 1
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.scok.arn
    container_name   = "scok-main"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.http,
    aws_ecs_cluster_capacity_providers.scok,
    aws_iam_role_policy_attachment.execution,
    aws_iam_role_policy.execution_secrets,
    aws_iam_role_policy.application,
    aws_s3_bucket_cors_configuration.originals
  ]
}