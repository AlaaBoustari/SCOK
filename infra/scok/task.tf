locals {
  application_environment = {
    SPRING_APPLICATION_NAME        = "SCOK"
    SPRING_DATASOURCE_URL          = "jdbc:postgresql://${aws_db_instance.scok.endpoint}/${aws_db_instance.scok.db_name}?sslmode=require"
    SPRING_DATASOURCE_USERNAME     = aws_db_instance.scok.username
    SPRING_JPA_HIBERNATE_DDLAUTO   = "update"
    APP_AWS_REGION                 = "eu-west-1"
    SPRING_CLOUD_AWS_REGION_STATIC = "eu-west-1"
    APP_S3_BUCKET                  = aws_s3_bucket.originals.id
    APP_S3_THUMBNAILBUCKET         = aws_s3_bucket.thumbnails.id
    APP_SQS_THUMBNAILQUEUEURL      = aws_sqs_queue.thumbnails.url
    SERVER_FORWARDHEADERSSTRATEGY  = "framework"

    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_PROVIDER                   = "cognito"
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_CLIENTID                   = var.cognito_client_id
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_CLIENTAUTHENTICATIONMETHOD = "client_secret_basic"
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_AUTHORIZATIONGRANTTYPE     = "authorization_code"
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_SCOPE                      = "openid,email,profile"
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_REDIRECTURI                = "{baseUrl}/login/oauth2/code/{registrationId}"
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_COGNITO_ISSUERURI                      = "https://cognito-idp.eu-west-1.amazonaws.com/${var.cognito_user_pool_id}"
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_COGNITO_USERNAMEATTRIBUTE              = "sub"
  }
}

resource "aws_ecs_task_definition" "scok" {
  family                   = "scok"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"

  cpu    = "1024"
  memory = "1536"

  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.application.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "scok-main"
      image     = "${aws_ecr_repository.scok.repository_url}:${var.image_tag}"
      essential = true

      portMappings = [{
        containerPort = 8080
        hostPort      = 0
        protocol      = "tcp"
      }]

      environment = [
        for name, value in local.application_environment : {
          name  = name
          value = value
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${aws_db_instance.scok.master_user_secret[0].secret_arn}:password::"
        },
        {
          name = "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_CLIENTSECRET"
          valueFrom = "${var.cognito_client_secret_arn}:cognito_client_secret::" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.scok.name
          "awslogs-region"        = "eu-west-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}