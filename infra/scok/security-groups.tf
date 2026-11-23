// So since I already clicked enough in the console
// I know what service calls which other service
// and thus can create the security groupes BASED
// on who calls who.

// Apparently Terraform creates a Dependency Graph
// to automatically know which file depend on which in a directory
// that's why i can call the default vpc i makde in network.tf

resource "aws_security_group" "alb" {
  name   = "scok-alb-sg"
  vpc_id = data.aws_vpc.default.id
}

resource "aws_security_group" "ecs_host" {
  name   = "scok-ecs-host-sg"
  vpc_id = data.aws_vpc.default.id
}

resource "aws_security_group" "rds" {
  name   = "scok-rds-sg"
  vpc_id = data.aws_vpc.default.id
}

// Recommended best practice says : Avoid using the ingress and egress arguments of the aws_security_group resource to configure in-line rules
// so we opt for the syntax below:


resource "aws_vpc_security_group_ingress_rule" "alb_from_internet" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_egress_rule" "alb_to_ecs" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.ecs_host.id
  ip_protocol                  = "tcp"
  from_port                    = 32768
  to_port                      = 61000
}

resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  security_group_id            = aws_security_group.ecs_host.id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = 32768
  to_port                      = 61000
}

resource "aws_vpc_security_group_egress_rule" "ecs_outbound" {
  security_group_id = aws_security_group.ecs_host.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_ecs" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.ecs_host.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}