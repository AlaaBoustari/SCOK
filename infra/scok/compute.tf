data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

resource "aws_launch_template" "ecs_host" {
  name_prefix   = "scok-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = "t3.small"

  iam_instance_profile {
    name = aws_iam_instance_profile.ecs_host.name
  }

  network_interfaces {
    device_index                = 0
    associate_public_ip_address = true
    security_groups             = [aws_security_group.ecs_host.id]
  }

  instance_market_options {
    market_type = "spot"
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = 30
      volume_type           = "gp3"
      delete_on_termination = true
    }
  }

  user_data = base64encode(<<-EOF
    #!/bin/bash
    echo "ECS_CLUSTER=${aws_ecs_cluster.scok.name}" >> /etc/ecs/ecs.config
  EOF
  )
}


resource "aws_autoscaling_group" "ecs" {
  name                = "scok-ecs-asg"
  min_size            = 1
  desired_capacity    = 2
  max_size            = 3
  vpc_zone_identifier = data.aws_subnets.default.ids

  launch_template {
    id      = aws_launch_template.ecs_host.id
    version = aws_launch_template.ecs_host.latest_version
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = "true"
    propagate_at_launch = true
  }

  depends_on = [
    aws_iam_role_policy_attachment.ecs_host,
    aws_vpc_security_group_egress_rule.ecs_outbound
  ]

  lifecycle {
    ignore_changes = [desired_capacity]
  }
}