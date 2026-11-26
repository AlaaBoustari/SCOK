resource "aws_ecr_repository" "scok" {
  name = "scok"
  force_delete = true

}

output "ecr_repository_url" {
  value = aws_ecr_repository.scok.repository_url
}

import {
  to = aws_ecr_repository.scok
  id = "scok"
}