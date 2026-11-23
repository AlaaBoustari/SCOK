resource "aws_db_subnet_group" "scok" {
  name       = "scok-db-subnets"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "scok" {
  identifier     = "scok-db"
  engine         = "postgres"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name                     = "scok"
  username                    = "scokadmin"
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.scok.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false
  multi_az            = false
  skip_final_snapshot = true
}