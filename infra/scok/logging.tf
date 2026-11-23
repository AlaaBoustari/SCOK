resource "aws_cloudwatch_log_group" "scok" {
  name              = "/ecs/scok"
  retention_in_days = 7
}